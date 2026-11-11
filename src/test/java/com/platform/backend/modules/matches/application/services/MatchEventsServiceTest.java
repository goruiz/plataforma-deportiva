package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.mappers.MatchEventMapper;
import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchEventType;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.matches.domain.exception.MatchEventError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchEventRequest.CreateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest.UpdateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchEventResponse.MatchEventResponse;
import com.platform.backend.modules.events.domain.exception.CategoryEligibilityError;
import com.platform.backend.modules.players.application.services.PlayerEligibilityPolicy;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchEventsServiceTest {

    private static final UUID CALLER_ID = UUID.randomUUID();

    @Mock private IMatchEventRepository matchEventRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamMemberRepository teamMemberRepository;
    @Mock private ITeamRepository teamRepository;
    // A mock answers "nobody is refused" until a test says otherwise.
    @Mock private PlayerEligibilityPolicy eligibilityPolicy;
    @Mock private MatchSettingsReader settingsReader;
    @Spy private MatchEventMapper matchEventMapper = Mappers.getMapper(MatchEventMapper.class);

    @InjectMocks
    private MatchEventsService matchEventsService;

    private MatchesEntity match;
    private TeamsEntity homeTeam;
    private PlayersEntity homePlayer;

    @BeforeEach
    void setUp() {
        homeTeam = team();
        homePlayer = player("Ana", "Lopez");

        match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setEventId(UUID.randomUUID());
        match.setHomeTeamId(homeTeam.getId());
        match.setAwayTeamId(UUID.randomUUID());

        lenient().when(matchRepository.findActiveById(match.getId())).thenReturn(Optional.of(match));
        lenient().when(playerRepository.findActiveById(homePlayer.getId())).thenReturn(Optional.of(homePlayer));
        lenient().when(teamMemberRepository.existsActiveByPlayerIdAndTeamId(homePlayer.getId(), homeTeam.getId())).thenReturn(true);
        lenient().when(matchEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(playerRepository.findFullNamesByIds(any()))
                .thenReturn(Map.of(homePlayer.getId(), homePlayer.getFirstName() + " " + homePlayer.getLastName()));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static TeamsEntity team() {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        return team;
    }

    private static PlayersEntity player(String firstName, String lastName) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setFirstName(firstName);
        player.setLastName(lastName);
        return player;
    }

    private CreateMatchEventRequest goalBy(PlayersEntity scorer, TeamsEntity team) {
        CreateMatchEventRequest request = new CreateMatchEventRequest();
        request.setMatchId(match.getId());
        request.setPlayerId(scorer.getId());
        request.setTeamId(team.getId());
        request.setEventType(MatchEventType.GOAL.name());
        request.setMinute(23);
        return request;
    }

    private MatchEventsEntity storedEvent(Integer minute, MatchEventType type, LocalDateTime createdAt) {
        MatchEventsEntity event = new MatchEventsEntity();
        event.setId(UUID.randomUUID());
        event.setMatchId(match.getId());
        event.setPlayerId(homePlayer.getId());
        event.setTeamId(homeTeam.getId());
        event.setEventType(type.name());
        event.setMinute(minute);
        event.setCreatedAt(createdAt);
        return event;
    }

    private static void assertRejectedField(Throwable error, String field, Enum<?> reason) {
        assertThat(error)
                .isInstanceOf(FieldValidationException.class)
                .satisfies(rejection -> assertThat(((FieldValidationException) rejection).getFieldErrors())
                        .containsExactly(entry(field, reason.name())));
    }

    @Test
    void create_whenTheEventIsValid_thenItIsSavedAndNamesThePlayer() {
        MatchEventResponse response = matchEventsService.create(goalBy(homePlayer, homeTeam));

        assertThat(response.getEventType()).isEqualTo("GOAL");
        assertThat(response.getMinute()).isEqualTo(23);
        assertThat(response.getPlayerName()).isEqualTo("Ana Lopez");
        verify(matchEventRepository).save(any(MatchEventsEntity.class));
    }

    @Test
    void create_whenTheTypeIsNotOneWeKnow_thenRefusedOnTheTypeField() {
        CreateMatchEventRequest request = goalBy(homePlayer, homeTeam);
        request.setEventType("PENALTY_SHOOTOUT");

        assertThatThrownBy(() -> matchEventsService.create(request))
                .satisfies(error -> assertRejectedField(error, "eventType", MatchEventError.INVALID_EVENT_TYPE));
        verify(matchEventRepository, never()).save(any());
    }

    @Test
    void create_whenTheTeamDidNotPlayTheMatch_thenRefusedOnTheTeamField() {
        CreateMatchEventRequest request = goalBy(homePlayer, team());

        assertThatThrownBy(() -> matchEventsService.create(request))
                .satisfies(error -> assertRejectedField(error, "teamId", MatchEventError.TEAM_NOT_IN_MATCH));
    }

    @Test
    void create_whenThePlayerBelongsToAnotherTeam_thenRefusedOnThePlayerField() {
        PlayersEntity stranger = player("Luis", "Mora");

        assertThatThrownBy(() -> matchEventsService.create(goalBy(stranger, homeTeam)))
                .satisfies(error -> assertRejectedField(error, "playerId", MatchEventError.PLAYER_NOT_IN_TEAM));
    }

    @Test
    void create_whenThePlayerHasNoTeamOrDoesNotExist_thenRefusedOnThePlayerField() {
        PlayersEntity free = player("Eva", "Rios");
        PlayersEntity ghost = player("Fantasma", "Nadie");

        assertThatThrownBy(() -> matchEventsService.create(goalBy(free, homeTeam)))
                .satisfies(error -> assertRejectedField(error, "playerId", MatchEventError.PLAYER_NOT_IN_TEAM));
        assertThatThrownBy(() -> matchEventsService.create(goalBy(ghost, homeTeam)))
                .satisfies(error -> assertRejectedField(error, "playerId", MatchEventError.PLAYER_NOT_IN_TEAM));
    }

    // ─── the category of the team: who may play ─────────────────────────────────────────────────────────────

    @Test
    void create_whenThePlayerDoesNotMeetTheCategoryOfTheTeam_thenRefusedOnThePlayerFieldWithTheReason() {
        when(teamRepository.findActiveById(homeTeam.getId())).thenReturn(Optional.of(homeTeam));
        when(eligibilityPolicy.reasonWhyNot(homePlayer, homeTeam))
                .thenReturn(Optional.of(CategoryEligibilityError.CATEGORY_TOO_YOUNG));

        assertThatThrownBy(() -> matchEventsService.create(goalBy(homePlayer, homeTeam)))
                .satisfies(error -> assertRejectedField(error, "playerId", CategoryEligibilityError.CATEGORY_TOO_YOUNG));
        verify(matchEventRepository, never()).save(any());
    }

    @Test
    void create_whenThePlayerMeetsTheCategoryOfTheTeam_thenItIsSaved() {
        when(teamRepository.findActiveById(homeTeam.getId())).thenReturn(Optional.of(homeTeam));
        when(eligibilityPolicy.reasonWhyNot(homePlayer, homeTeam)).thenReturn(Optional.empty());

        assertThat(matchEventsService.create(goalBy(homePlayer, homeTeam)).getEventType()).isEqualTo("GOAL");
    }

    // ─── substitutions: who goes out and how many the settings allow ────────────────────────────────────────

    private PlayersEntity benchPlayer() {
        PlayersEntity bench = player("Luis", "Mora");
        lenient().when(teamMemberRepository.existsActiveByPlayerIdAndTeamId(bench.getId(), homeTeam.getId())).thenReturn(true);
        lenient().when(playerRepository.findFullNamesByIds(any())).thenReturn(Map.of(
                homePlayer.getId(), "Ana Lopez", bench.getId(), "Luis Mora"));
        return bench;
    }

    private void allowSubstitutions(int perTeam) {
        when(settingsReader.of(match)).thenReturn(new SettingsValues(15, perTeam, true, 0, false, 48));
    }

    private CreateMatchEventRequest substitution(PlayersEntity comesIn, PlayersEntity goesOut) {
        CreateMatchEventRequest request = goalBy(comesIn, homeTeam);
        request.setEventType(MatchEventType.SUBSTITUTION.name());
        request.setReplacedPlayerId(goesOut == null ? null : goesOut.getId());
        return request;
    }

    private void alreadyMadeSubstitutions(int count) {
        List<MatchEventsEntity> made = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> storedEvent(10 + index, MatchEventType.SUBSTITUTION, LocalDateTime.now())).toList();
        when(matchEventRepository.findAllActiveByMatchId(match.getId())).thenReturn(made);
    }

    @Test
    void create_whenASubstitutionIsWithinTheLimit_thenItIsSavedAndNamesBothPlayers() {
        PlayersEntity bench = benchPlayer();
        allowSubstitutions(2);
        alreadyMadeSubstitutions(1);

        MatchEventResponse response = matchEventsService.create(substitution(bench, homePlayer));

        assertThat(response.getEventType()).isEqualTo("SUBSTITUTION");
        assertThat(response.getPlayerName()).isEqualTo("Luis Mora");
        assertThat(response.getReplacedPlayerId()).isEqualTo(homePlayer.getId());
        assertThat(response.getReplacedPlayerName()).isEqualTo("Ana Lopez");
    }

    @Test
    void create_whenTheTeamAlreadyMadeAllItsSubstitutions_thenRefusedOnTheTypeField() {
        PlayersEntity bench = benchPlayer();
        allowSubstitutions(2);
        alreadyMadeSubstitutions(2);

        assertThatThrownBy(() -> matchEventsService.create(substitution(bench, homePlayer)))
                .satisfies(error -> assertRejectedField(error, "eventType", MatchEventError.SUBSTITUTION_LIMIT_REACHED));
        verify(matchEventRepository, never()).save(any());
    }

    @Test
    void create_whenTheSettingsAllowNoSubstitutions_thenTheFirstOneIsRefused() {
        PlayersEntity bench = benchPlayer();
        allowSubstitutions(0);

        assertThatThrownBy(() -> matchEventsService.create(substitution(bench, homePlayer)))
                .satisfies(error -> assertRejectedField(error, "eventType", MatchEventError.SUBSTITUTION_LIMIT_REACHED));
    }

    @Test
    void create_whenTheOtherTeamMadeItsSubstitutions_thenThisTeamStillHasItsOwn() {
        PlayersEntity bench = benchPlayer();
        allowSubstitutions(1);
        MatchEventsEntity ofTheOtherTeam = storedEvent(10, MatchEventType.SUBSTITUTION, LocalDateTime.now());
        ofTheOtherTeam.setTeamId(match.getAwayTeamId());
        when(matchEventRepository.findAllActiveByMatchId(match.getId())).thenReturn(List.of(ofTheOtherTeam));

        assertThat(matchEventsService.create(substitution(bench, homePlayer)).getEventType()).isEqualTo("SUBSTITUTION");
    }

    @Test
    void create_whenASubstitutionDoesNotSayWhoGoesOut_thenRefusedOnThatField() {
        PlayersEntity bench = benchPlayer();

        assertThatThrownBy(() -> matchEventsService.create(substitution(bench, null)))
                .satisfies(error -> assertRejectedField(error, "replacedPlayerId", MatchEventError.REPLACED_PLAYER_REQUIRED));
    }

    @Test
    void create_whenTheSamePlayerComesInAndGoesOut_thenRefusedOnThatField() {
        assertThatThrownBy(() -> matchEventsService.create(substitution(homePlayer, homePlayer)))
                .satisfies(error -> assertRejectedField(error, "replacedPlayerId", MatchEventError.REPLACED_PLAYER_SAME_AS_PLAYER));
    }

    @Test
    void create_whenWhoGoesOutIsNotOfTheTeam_thenRefusedOnThatField() {
        PlayersEntity stranger = player("Eva", "Rios");

        assertThatThrownBy(() -> matchEventsService.create(substitution(homePlayer, stranger)))
                .satisfies(error -> assertRejectedField(error, "replacedPlayerId", MatchEventError.PLAYER_NOT_IN_TEAM));
    }

    @Test
    void create_whenItIsNotASubstitution_thenWhoGoesOutIsDropped() {
        CreateMatchEventRequest request = goalBy(homePlayer, homeTeam);
        request.setReplacedPlayerId(UUID.randomUUID());

        assertThat(matchEventsService.create(request).getReplacedPlayerId()).isNull();
    }

    @Test
    void update_whenASubstitutionIsChangedWithTheLimitReached_thenItDoesNotCountAgainstItself() {
        PlayersEntity bench = benchPlayer();
        allowSubstitutions(1);
        MatchEventsEntity stored = storedEvent(10, MatchEventType.SUBSTITUTION, LocalDateTime.now());
        stored.setPlayerId(bench.getId());
        stored.setReplacedPlayerId(homePlayer.getId());
        when(matchEventRepository.findActiveById(stored.getId())).thenReturn(Optional.of(stored));
        when(matchEventRepository.findAllActiveByMatchId(match.getId())).thenReturn(List.of(stored));
        UpdateMatchEventRequest request = new UpdateMatchEventRequest();
        request.setMinute(55);

        assertThat(matchEventsService.update(stored.getId(), request).getMinute()).isEqualTo(55);
    }

    @Test
    void create_whenTheMatchDoesNotExist_thenRefusedOnTheMatchField() {
        CreateMatchEventRequest request = goalBy(homePlayer, homeTeam);
        request.setMatchId(UUID.randomUUID());

        assertThatThrownBy(() -> matchEventsService.create(request))
                .satisfies(error -> assertRejectedField(error, "matchId", MatchError.MATCH_NOT_FOUND));
    }

    @Test
    void create_whenAMobileAccountDidNotCreateTheEvent_thenForbiddenAndNothingIsSaved() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");
        EventsEntity event = new EventsEntity();
        event.setCreatedBy(UUID.randomUUID());
        when(eventRepository.findActiveById(match.getEventId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> matchEventsService.create(goalBy(homePlayer, homeTeam)))
                .isInstanceOf(AccessDeniedException.class);
        verify(matchEventRepository, never()).save(any());
    }

    @Test
    void create_whenAMobileAccountCreatedTheEvent_thenItIsSaved() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");
        EventsEntity event = new EventsEntity();
        event.setCreatedBy(CALLER_ID);
        when(eventRepository.findActiveById(match.getEventId())).thenReturn(Optional.of(event));

        assertThat(matchEventsService.create(goalBy(homePlayer, homeTeam)).getEventType()).isEqualTo("GOAL");
    }

    @Test
    void update_whenOnlyTheMinuteChanges_thenTheRestStaysAndItIsValidAgainstTheStoredValues() {
        MatchEventsEntity stored = storedEvent(10, MatchEventType.GOAL, LocalDateTime.now());
        when(matchEventRepository.findActiveById(stored.getId())).thenReturn(Optional.of(stored));
        UpdateMatchEventRequest request = new UpdateMatchEventRequest();
        request.setMinute(44);

        MatchEventResponse response = matchEventsService.update(stored.getId(), request);

        assertThat(response.getMinute()).isEqualTo(44);
        assertThat(response.getEventType()).isEqualTo("GOAL");
        assertThat(response.getTeamId()).isEqualTo(homeTeam.getId());
    }

    @Test
    void update_whenTheNewTypeIsInvalid_thenRefusedAndNothingIsSaved() {
        MatchEventsEntity stored = storedEvent(10, MatchEventType.GOAL, LocalDateTime.now());
        when(matchEventRepository.findActiveById(stored.getId())).thenReturn(Optional.of(stored));
        UpdateMatchEventRequest request = new UpdateMatchEventRequest();
        request.setEventType("DIVE");

        assertThatThrownBy(() -> matchEventsService.update(stored.getId(), request))
                .satisfies(error -> assertRejectedField(error, "eventType", MatchEventError.INVALID_EVENT_TYPE));
        verify(matchEventRepository, never()).save(any());
    }

    @Test
    void delete_whenAMobileAccountDidNotCreateTheEvent_thenForbidden() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");
        MatchEventsEntity stored = storedEvent(10, MatchEventType.GOAL, LocalDateTime.now());
        when(matchEventRepository.findActiveById(stored.getId())).thenReturn(Optional.of(stored));
        EventsEntity event = new EventsEntity();
        event.setCreatedBy(UUID.randomUUID());
        when(eventRepository.findActiveById(match.getEventId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> matchEventsService.delete(stored.getId())).isInstanceOf(AccessDeniedException.class);
        verify(matchEventRepository, never()).delete(any());
    }

    @Test
    void getByMatchId_whenCalled_thenTheTimelineFollowsTheMinutesAndAnEventWithoutMinuteGoesLast() {
        LocalDateTime now = LocalDateTime.now();
        MatchEventsEntity late = storedEvent(70, MatchEventType.YELLOW_CARD, now);
        MatchEventsEntity noMinute = storedEvent(null, MatchEventType.FOUL, now.minusMinutes(5));
        MatchEventsEntity early = storedEvent(5, MatchEventType.GOAL, now.plusMinutes(5));
        when(matchEventRepository.findAllActiveByMatchId(match.getId())).thenReturn(List.of(late, noMinute, early));

        List<MatchEventResponse> timeline = matchEventsService.getByMatchId(match.getId());

        assertThat(timeline).extracting(MatchEventResponse::getEventType)
                .containsExactly("GOAL", "YELLOW_CARD", "FOUL");
        assertThat(timeline).extracting(MatchEventResponse::getPlayerName).containsOnly("Ana Lopez");
    }

    @Test
    void getByMatchId_whenTheMatchDoesNotExist_thenNotFound() {
        assertThatThrownBy(() -> matchEventsService.getByMatchId(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
