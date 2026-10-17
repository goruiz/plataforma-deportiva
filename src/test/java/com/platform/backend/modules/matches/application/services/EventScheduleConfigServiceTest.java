package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.holidays.application.iservices.IHolidaysService;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse.GenerateMatchesResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventScheduleConfigServiceTest {

    // Far in the future, so "matches are never placed in the past" doesn't interfere unless a test asks for it.
    private static final LocalDate EVENT_START = LocalDate.of(2099, 3, 1);
    private static final LocalDate FIRST_SATURDAY = EVENT_START.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

    @Mock private IEventScheduleConfigRepository configRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private ITeamsEventsRepository teamsEventsRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IHolidaysService holidaysService;
    @Mock private IMatchesService matchesService;
    @Spy private MatchMapper matchMapper = Mappers.getMapper(MatchMapper.class);

    @InjectMocks
    private EventScheduleConfigService service;

    private UUID eventId;
    private EventsEntity event;
    private EventScheduleConfigEntity config;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();

        event = new EventsEntity();
        event.setId(eventId);
        event.setStartDate(EVENT_START);
        event.setEndDate(EVENT_START.plusYears(1));

        // Saturdays only, 09:00, 60-minute matches with 15 minutes in between: two matches per round fit 09:00 and 10:15.
        config = new EventScheduleConfigEntity();
        config.setEventId(eventId);
        config.setPlayDays("SATURDAY");
        config.setStartTime(LocalTime.of(9, 0));
        config.setMatchDurationMinutes(60);
        config.setBreakBetweenMatchesMinutes(15);

        lenient().when(eventRepository.findActiveById(eventId)).thenReturn(Optional.of(event));
        lenient().when(configRepository.findActiveByEventId(eventId)).thenReturn(Optional.of(config));
        lenient().when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of());
        lenient().when(holidaysService.getNonPlayDatesBetween(any(), any())).thenReturn(Set.of());
        lenient().when(matchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private static UUID team(int number) {
        return new UUID(0, number);
    }

    private void registerTeams(int count) {
        List<TeamsEventsEntity> registrations = IntStream.rangeClosed(1, count).mapToObj(number -> {
            TeamsEntity team = new TeamsEntity();
            team.setId(team(number));
            TeamsEventsEntity registration = new TeamsEventsEntity();
            registration.setTeam(team);
            return registration;
        }).toList();
        when(teamsEventsRepository.findAllActiveByEventId(eventId)).thenReturn(registrations);
    }

    private static MatchesEntity existingMatch(int home, int away, LocalDateTime date, String status, Integer homeScore, Integer awayScore) {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setHomeTeamId(team(home));
        match.setAwayTeamId(team(away));
        match.setMatchDate(date);
        match.setStatus(status);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        return match;
    }

    private static MatchesEntity pendingMatch(int home, int away) {
        return existingMatch(home, away, FIRST_SATURDAY.atTime(9, 0), "SCHEDULED", null, null);
    }

    private static Map<LocalDate, List<MatchResponse>> byDate(List<MatchResponse> matches) {
        return matches.stream().collect(Collectors.groupingBy(
                match -> match.getMatchDate().toLocalDate(), TreeMap::new, Collectors.toList()));
    }

    private static Set<UUID> pairOf(MatchResponse match) {
        return Set.of(match.getHomeTeamId(), match.getAwayTeamId());
    }

    // ─── a fresh calendar ───────────────────────────────────────────────────────

    @Test
    void generate_whenFourTeams_thenEveryPairingIsScheduledRoundAfterRoundOnSuccessivePlayDays() {
        registerTeams(4);

        GenerateMatchesResponse response = service.generateMatches(eventId);

        Map<LocalDate, List<MatchResponse>> days = byDate(response.getMatches());
        assertThat(response.getMatches()).hasSize(6);
        assertThat(days.keySet()).containsExactly(FIRST_SATURDAY, FIRST_SATURDAY.plusWeeks(1), FIRST_SATURDAY.plusWeeks(2));
        days.values().forEach(dayMatches -> {
            assertThat(dayMatches).hasSize(2);
            Set<UUID> teamsOfTheDay = dayMatches.stream().flatMap(match -> pairOf(match).stream()).collect(Collectors.toSet());
            assertThat(teamsOfTheDay).as("no team plays twice on the same day").hasSize(4);
        });
        assertThat(response.getMatches()).extracting(MatchResponse::getStatus).containsOnly("SCHEDULED");
    }

    @Test
    void generate_whenSeveralMatchesShareADay_thenTheyFollowEachOtherWithTheBreakInBetween() {
        registerTeams(4);

        List<MatchResponse> firstDay = byDate(service.generateMatches(eventId).getMatches()).get(FIRST_SATURDAY);

        assertThat(firstDay).extracting(match -> match.getMatchDate().toLocalTime())
                .containsExactlyInAnyOrder(LocalTime.of(9, 0), LocalTime.of(10, 15));
    }

    @Test
    void generate_whenTheConfigHasACourt_thenEveryMatchUsesIt() {
        UUID courtId = UUID.randomUUID();
        config.setCourtId(courtId);
        registerTeams(4);

        assertThat(service.generateMatches(eventId).getMatches()).extracting(MatchResponse::getCourtId).containsOnly(courtId);
    }

    @Test
    void generate_whenAllPairingsFitBeforeTheEventEnds_thenNoWarning() {
        registerTeams(4);

        assertThat(service.generateMatches(eventId).getWarning()).isNull();
    }

    @Test
    void generate_whenTheLastMatchFallsAfterTheEventEnd_thenWarningWithThatDate() {
        event.setEndDate(FIRST_SATURDAY.plusDays(1)); // three rounds need three Saturdays
        registerTeams(4);

        assertThat(service.generateMatches(eventId).getWarning()).contains(FIRST_SATURDAY.plusWeeks(2).toString());
    }

    // ─── matches that ended by themselves ───────────────────────────────────────

    @Test
    void generate_beforeLookingAtTheExistingMatches_thenTheOnesWhoseTimeIsOverAreFinishedSoTheyAreKept() {
        registerTeams(4);

        service.generateMatches(eventId);

        InOrder order = inOrder(matchesService, matchRepository);
        order.verify(matchesService).finishEndedMatches(eventId);
        order.verify(matchRepository).findAllActiveByEventId(eventId);
    }

    @Test
    void generate_whenMatchesAreCreated_thenEachCarriesWhenItEnds() {
        registerTeams(4);

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        // 60 minutes of play + 15 of break
        assertThat(matches).allSatisfy(match -> assertThat(match.getEndsAt()).isEqualTo(match.getMatchDate().plusMinutes(75)));
    }

    // ─── holidays and blocked days ──────────────────────────────────────────────

    @Test
    void generate_whenAHolidayFallsOnAPlayDay_thenThatDayIsSkipped() {
        LocalDate holiday = FIRST_SATURDAY.plusWeeks(1);
        when(holidaysService.getNonPlayDatesBetween(any(), any())).thenReturn(Set.of(holiday));
        registerTeams(4);

        Set<LocalDate> days = byDate(service.generateMatches(eventId).getMatches()).keySet();

        assertThat(days).containsExactly(FIRST_SATURDAY, FIRST_SATURDAY.plusWeeks(2), FIRST_SATURDAY.plusWeeks(3));
    }

    @Test
    void generate_whenTheEventBlocksADate_thenThatDayIsSkipped() {
        config.setBlockedDates(FIRST_SATURDAY.toString());
        registerTeams(4);

        Set<LocalDate> days = byDate(service.generateMatches(eventId).getMatches()).keySet();

        assertThat(days).doesNotContain(FIRST_SATURDAY);
        assertThat(days).first().isEqualTo(FIRST_SATURDAY.plusWeeks(1));
    }

    @Test
    void generate_asksForTheHolidaysOfTheWholeSearchWindowStartingAtTheFirstSchedulableDay() {
        registerTeams(4);

        service.generateMatches(eventId);

        verify(holidaysService).getNonPlayDatesBetween(EVENT_START, EVENT_START.plusYears(5));
    }

    // ─── never in the past ──────────────────────────────────────────────────────

    @Test
    void generate_whenTheEventAlreadyStarted_thenNoMatchIsPlacedBeforeToday() {
        event.setStartDate(LocalDate.now().minusDays(30));
        config.setPlayDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        registerTeams(4);

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).isNotEmpty();
        assertThat(matches).allSatisfy(match -> assertThat(match.getMatchDate().toLocalDate()).isAfterOrEqualTo(LocalDate.now()));
    }

    @Test
    void generate_whenTheEventHasNoStartDate_thenItStartsToday() {
        event.setStartDate(null);
        config.setPlayDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        registerTeams(4);

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).allSatisfy(match -> assertThat(match.getMatchDate().toLocalDate()).isAfterOrEqualTo(LocalDate.now()));
    }

    // ─── generating again ───────────────────────────────────────────────────────

    @Test
    void generate_whenMatchesAreStillPending_thenTheyAreReplacedInsteadOfDuplicated() {
        registerTeams(4);
        List<MatchesEntity> pending = List.of(pendingMatch(1, 2), pendingMatch(3, 4), pendingMatch(1, 3));
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(pending);

        GenerateMatchesResponse response = service.generateMatches(eventId);

        ArgumentCaptor<MatchesEntity> discarded = ArgumentCaptor.forClass(MatchesEntity.class);
        verify(matchRepository, org.mockito.Mockito.times(3)).delete(discarded.capture());
        assertThat(discarded.getAllValues()).containsExactlyInAnyOrderElementsOf(pending);
        assertThat(discarded.getAllValues()).allSatisfy(match -> assertThat(match.getDeletedAt()).isNotNull());
        assertThat(response.getMatches()).as("the whole calendar is built again, once").hasSize(6);
    }

    @Test
    void generate_whenSomePairingsWerePlayed_thenTheyAreKeptAndNotScheduledAgain() {
        registerTeams(4);
        MatchesEntity played = existingMatch(2, 1, LocalDate.now().minusDays(7).atTime(9, 0), "FINISHED", 2, 1);
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(played, pendingMatch(3, 4)));

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).hasSize(5);
        assertThat(matches).extracting(EventScheduleConfigServiceTest::pairOf).doesNotContain(Set.of(team(1), team(2)));
        verify(matchRepository, never()).delete(played);
        assertThat(played.getDeletedAt()).isNull();
    }

    @Test
    void generate_whenAMatchHasAResultEvenIfItsStatusWasNeverUpdated_thenItCountsAsPlayed() {
        registerTeams(4);
        MatchesEntity withResult = existingMatch(1, 2, LocalDate.now().minusDays(7).atTime(9, 0), "SCHEDULED", 0, 0);
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(withResult));

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).hasSize(5);
        verify(matchRepository, never()).delete(withResult);
    }

    @Test
    void generate_whenACalledOffMatchExists_thenItsPairingIsNotScheduledAgain() {
        registerTeams(4);
        MatchesEntity cancelled = existingMatch(3, 4, FIRST_SATURDAY.atTime(9, 0), "CANCELLED", null, null);
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(cancelled));

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).hasSize(5);
        assertThat(matches).extracting(EventScheduleConfigServiceTest::pairOf).doesNotContain(Set.of(team(3), team(4)));
    }

    @Test
    void generate_whenAKeptMatchOccupiesADay_thenItsTeamsDoNotPlayAgainThatDay() {
        registerTeams(4);
        MatchesEntity keptOnFirstSaturday = existingMatch(1, 2, FIRST_SATURDAY.atTime(9, 0), "IN_PROGRESS", null, null);
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(keptOnFirstSaturday));

        List<MatchResponse> firstSaturday = byDate(service.generateMatches(eventId).getMatches())
                .getOrDefault(FIRST_SATURDAY, List.of());

        Set<UUID> teamsThatDay = firstSaturday.stream().flatMap(match -> pairOf(match).stream()).collect(Collectors.toSet());
        assertThat(teamsThatDay).doesNotContain(team(1), team(2));
    }

    @Test
    void generate_whenEverythingWasAlreadyPlayed_thenNothingIsCreated() {
        registerTeams(3);
        List<MatchesEntity> allPlayed = new ArrayList<>(List.of(
                existingMatch(1, 2, LocalDate.now().minusDays(21).atTime(9, 0), "FINISHED", 1, 0),
                existingMatch(1, 3, LocalDate.now().minusDays(14).atTime(9, 0), "FINISHED", 1, 0),
                existingMatch(2, 3, LocalDate.now().minusDays(7).atTime(9, 0), "FINISHED", 1, 0)));
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(allPlayed);

        GenerateMatchesResponse response = service.generateMatches(eventId);

        assertThat(response.getMatches()).isEmpty();
        assertThat(response.getWarning()).isNull();
        verify(matchRepository, never()).save(any());
    }

    // ─── a day has a limit ──────────────────────────────────────────────────────

    @Test
    void generate_whenADayCannotHoldAnotherMatchBeforeMidnight_thenItGoesToTheNextPlayDay() {
        config.setStartTime(LocalTime.of(20, 0));
        config.setMatchDurationMinutes(120);
        config.setBreakBetweenMatchesMinutes(0);
        registerTeams(6); // 3 matches per round, but only 20:00 and 22:00 fit before midnight

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).hasSize(15);
        assertThat(matches).extracting(match -> match.getMatchDate().toLocalTime())
                .containsOnly(LocalTime.of(20, 0), LocalTime.of(22, 0));
        byDate(matches).values().forEach(dayMatches -> assertThat(dayMatches).hasSizeLessThanOrEqualTo(2));
    }

    @Test
    void generate_whenNotEvenOneMatchFitsADay_thenItFailsMentioningThePlayDays() {
        config.setStartTime(LocalTime.of(23, 30));
        config.setMatchDurationMinutes(120);
        registerTeams(4);

        assertThatThrownBy(() -> service.generateMatches(eventId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("play days"); // the mobile app recognises the failure by this text
    }

    // ─── preconditions ──────────────────────────────────────────────────────────

    @Test
    void generate_whenTheEventHasFewerThanTwoTeams_thenItFails() {
        registerTeams(1);

        assertThatThrownBy(() -> service.generateMatches(eventId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least 2 teams");
        verify(matchRepository, never()).save(any());
    }

    @Test
    void generate_whenThereIsNoConfigYet_thenNotFound() {
        when(configRepository.findActiveByEventId(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateMatches(eventId)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void generate_whenTheEventDoesNotExist_thenNotFound() {
        when(eventRepository.findActiveById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateMatches(eventId)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void generate_whenAPlayDayIsNotADayOfTheWeek_thenItFails() {
        config.setPlayDays("FUNDAY");
        registerTeams(4);

        assertThatThrownBy(() -> service.generateMatches(eventId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_neverUsesTheSameDayTwiceForATeamAcrossAFullCalendar() {
        registerTeams(7);

        List<MatchResponse> matches = service.generateMatches(eventId).getMatches();

        assertThat(matches).hasSize(21);
        Set<String> teamDayPairs = new HashSet<>();
        for (MatchResponse match : matches) {
            LocalDate day = match.getMatchDate().toLocalDate();
            assertThat(teamDayPairs.add(match.getHomeTeamId() + "@" + day)).isTrue();
            assertThat(teamDayPairs.add(match.getAwayTeamId() + "@" + day)).isTrue();
        }
    }
}
