package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IMatchTimingService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest.CreateMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchesServiceTest {

    // 75 minutes: a 60-minute match with a 15-minute break, the slot the calendar generator reserves.
    private static final int SLOT_MINUTES = 75;

    @Mock private IMatchRepository matchRepository;
    @Mock private IMatchTimingService matchTimingService;
    @Mock private IEventRepository eventRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private ICourtRepository courtRepository;
    @Spy private MatchMapper matchMapper = Mappers.getMapper(MatchMapper.class);

    @InjectMocks
    private MatchesService matchesService;

    @BeforeEach
    void setUp() {
        // The timing service is a collaborator with its own tests: here it just applies the slot to each match.
        lenient().when(matchTimingService.getEndTimes(any())).thenAnswer(invocation -> {
            Collection<MatchesEntity> matches = invocation.getArgument(0);
            Map<UUID, LocalDateTime> endTimes = new HashMap<>();
            matches.forEach(match -> endTimes.put(match.getId(), match.getMatchDate().plusMinutes(SLOT_MINUTES)));
            return endTimes;
        });
        lenient().when(matchTimingService.hasEnded(any(), any())).thenAnswer(invocation -> {
            MatchesEntity match = invocation.getArgument(0);
            LocalDateTime now = invocation.getArgument(1);
            return !match.getMatchDate().plusMinutes(SLOT_MINUTES).isAfter(now);
        });
        lenient().when(matchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static MatchesEntity match(LocalDateTime kickOff, String status) {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setEventId(UUID.randomUUID());
        match.setHomeTeamId(UUID.randomUUID());
        match.setAwayTeamId(UUID.randomUUID());
        match.setMatchDate(kickOff);
        match.setStatus(status);
        return match;
    }

    private static UpdateMatchRequest scores(Integer home, Integer away) {
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setHomeScore(home);
        request.setAwayScore(away);
        return request;
    }

    private MatchesEntity storedMatch(LocalDateTime kickOff, String status) {
        MatchesEntity match = match(kickOff, status);
        when(matchRepository.findActiveById(match.getId())).thenReturn(Optional.of(match));
        return match;
    }

    private static LocalDateTime hoursAgo(int hours) {
        return LocalDateTime.now().minusHours(hours);
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    // ─── the event, teams and court of a match must be the caller's own ─────────

    private static final UUID OWN_EVENT_ID = UUID.randomUUID();
    private static final UUID OWN_HOME_TEAM_ID = UUID.randomUUID();
    private static final UUID OWN_AWAY_TEAM_ID = UUID.randomUUID();
    private static final UUID OWN_COURT_ID = UUID.randomUUID();

    private void givenTheOrganizationOwnsTheEventTheTeamsAndTheCourt() {
        lenient().when(eventRepository.findActiveById(OWN_EVENT_ID)).thenReturn(Optional.of(new EventsEntity()));
        lenient().when(teamRepository.findActiveById(OWN_HOME_TEAM_ID)).thenReturn(Optional.of(new TeamsEntity()));
        lenient().when(teamRepository.findActiveById(OWN_AWAY_TEAM_ID)).thenReturn(Optional.of(new TeamsEntity()));
        lenient().when(courtRepository.findActiveById(OWN_COURT_ID)).thenReturn(Optional.of(new CourtsEntity()));
    }

    private static CreateMatchRequest newMatchOf(UUID eventId, UUID homeTeamId, UUID awayTeamId, UUID courtId) {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setEventId(eventId);
        request.setHomeTeamId(homeTeamId);
        request.setAwayTeamId(awayTeamId);
        request.setCourtId(courtId);
        request.setMatchDate(LocalDateTime.now().plusDays(3));
        return request;
    }

    @Test
    void create_whenAnOrganizationAdministratorUsesTheirOwnEventTeamsAndCourt_thenTheMatchIsSaved() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        givenTheOrganizationOwnsTheEventTheTeamsAndTheCourt();

        matchesService.create(newMatchOf(OWN_EVENT_ID, OWN_HOME_TEAM_ID, OWN_AWAY_TEAM_ID, OWN_COURT_ID));

        verify(matchRepository).save(any(MatchesEntity.class));
    }

    @Test
    void create_whenAnOrganizationAdministratorUsesAnEventOfAnotherOrganization_thenRejectTheEventWithoutSaving() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        givenTheOrganizationOwnsTheEventTheTeamsAndTheCourt();
        UUID foreignEventId = UUID.randomUUID();
        when(eventRepository.findActiveById(foreignEventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchesService.create(
                newMatchOf(foreignEventId, OWN_HOME_TEAM_ID, OWN_AWAY_TEAM_ID, null)))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("eventId", "EVENT_NOT_FOUND")));

        verify(matchRepository, never()).save(any());
    }

    @Test
    void create_whenAnOrganizationAdministratorSendsNoEvent_thenRejectTheEvent() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatThrownBy(() -> matchesService.create(newMatchOf(null, OWN_HOME_TEAM_ID, OWN_AWAY_TEAM_ID, null)))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("eventId", "EVENT_NOT_FOUND")));
    }

    @Test
    void create_whenAnOrganizationAdministratorUsesATeamOfAnotherOrganization_thenRejectThatTeam() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        givenTheOrganizationOwnsTheEventTheTeamsAndTheCourt();
        UUID foreignTeamId = UUID.randomUUID();
        when(teamRepository.findActiveById(foreignTeamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchesService.create(newMatchOf(OWN_EVENT_ID, OWN_HOME_TEAM_ID, foreignTeamId, null)))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("awayTeamId", "TEAM_NOT_FOUND")));
    }

    @Test
    void create_whenAnOrganizationAdministratorUsesACourtOfAnotherOrganization_thenRejectThatCourt() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        givenTheOrganizationOwnsTheEventTheTeamsAndTheCourt();
        UUID foreignCourtId = UUID.randomUUID();
        when(courtRepository.findActiveById(foreignCourtId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchesService.create(
                newMatchOf(OWN_EVENT_ID, OWN_HOME_TEAM_ID, OWN_AWAY_TEAM_ID, foreignCourtId)))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("courtId", "COURT_NOT_FOUND")));
    }

    @Test
    void create_whenTheCallerSeesEverything_thenTheReferencesAreNotLookedUp() {
        TenantTestSupport.actAsPlatformOwner();

        matchesService.create(newMatchOf(null, OWN_HOME_TEAM_ID, OWN_AWAY_TEAM_ID, null));

        verifyNoInteractions(eventRepository, teamRepository, courtRepository);
        verify(matchRepository).save(any(MatchesEntity.class));
    }

    @Test
    void update_whenAnOrganizationAdministratorMovesTheMatchToAnotherOrganizationsEvent_thenRejectTheEvent() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        MatchesEntity stored = storedMatch(LocalDateTime.now().plusDays(2), "SCHEDULED");
        UUID foreignEventId = UUID.randomUUID();
        when(eventRepository.findActiveById(foreignEventId)).thenReturn(Optional.empty());
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setEventId(foreignEventId);

        assertThatThrownBy(() -> matchesService.update(stored.getId(), request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("eventId", "EVENT_NOT_FOUND")));

        verify(matchRepository, never()).save(any());
    }

    @Test
    void update_whenAnOrganizationAdministratorOnlyChangesTheScore_thenNoReferenceIsLookedUp() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        MatchesEntity stored = storedMatch(hoursAgo(5), "SCHEDULED");

        matchesService.update(stored.getId(), scores(2, 1));

        verifyNoInteractions(eventRepository, teamRepository, courtRepository);
        assertThat(stored.getHomeScore()).isEqualTo(2);
    }

    // ─── recording a result ─────────────────────────────────────────────────────

    @Test
    void update_whenAResultIsRecordedBeforeTheMatchEnds_thenRefusedWithTheCodeOnTheScoreField() {
        MatchesEntity inProgressSlot = storedMatch(LocalDateTime.now().minusMinutes(30), "SCHEDULED"); // ends in 45 minutes

        assertThatThrownBy(() -> matchesService.update(inProgressSlot.getId(), scores(2, 1)))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("homeScore", MatchError.RESULT_BEFORE_MATCH_END.name()));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void update_whenOnlyTheAwayScoreIsSentBeforeTheEnd_thenTheErrorIsOnTheAwayScoreField() {
        MatchesEntity future = storedMatch(LocalDateTime.now().plusHours(2), "SCHEDULED");

        assertThatThrownBy(() -> matchesService.update(future.getId(), scores(null, 3)))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsKey("awayScore"));
    }

    @Test
    void update_whenAResultIsRecordedOnceTheMatchEnded_thenItIsSavedAndTheMatchIsFinished() {
        MatchesEntity played = storedMatch(hoursAgo(3), "SCHEDULED");

        MatchResponse response = matchesService.update(played.getId(), scores(2, 1));

        assertThat(response.getStatus()).isEqualTo("FINISHED");
        assertThat(response.getHomeScore()).isEqualTo(2);
        assertThat(response.getAwayScore()).isEqualTo(1);
        verify(matchRepository).save(played);
    }

    @Test
    void update_whenAResultIsRecordedExactlyWhenTheMatchEnds_thenItIsAccepted() {
        // kick-off 75 minutes ago: the slot is over this very minute (the rule is "from 11:15", not "after")
        MatchesEntity justEnded = storedMatch(LocalDateTime.now().minusMinutes(SLOT_MINUTES).minusSeconds(1), "SCHEDULED");

        assertThat(matchesService.update(justEnded.getId(), scores(0, 0)).getStatus()).isEqualTo("FINISHED");
    }

    @Test
    void update_whenTheRequestSetsAStatusItself_thenItIsRespectedEvenIfAResultComesWithIt() {
        MatchesEntity played = storedMatch(hoursAgo(3), "SCHEDULED");
        UpdateMatchRequest request = scores(3, 0);
        request.setStatus("COMPLETED");

        assertThat(matchesService.update(played.getId(), request).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void update_whenAPostponedMatchGetsAResult_thenItStaysPostponedBecauseItWasNotPlayedAsScheduled() {
        MatchesEntity postponed = storedMatch(hoursAgo(3), "POSTPONED");

        assertThat(matchesService.update(postponed.getId(), scores(1, 1)).getStatus()).isEqualTo("POSTPONED");
    }

    @Test
    void update_whenNoResultIsSent_thenAFutureMatchCanStillBeEdited() {
        MatchesEntity future = storedMatch(LocalDateTime.now().plusDays(2), "SCHEDULED");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setLocation("Estadio Norte");

        MatchResponse response = matchesService.update(future.getId(), request);

        assertThat(response.getLocation()).isEqualTo("Estadio Norte");
        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void update_whenTheRequestMovesTheMatchIntoTheFutureAndSendsAResult_thenRefused() {
        MatchesEntity played = storedMatch(hoursAgo(3), "FINISHED");
        UpdateMatchRequest request = scores(2, 1);
        request.setMatchDate(LocalDateTime.now().plusDays(5));

        assertThatThrownBy(() -> matchesService.update(played.getId(), request)).isInstanceOf(FieldValidationException.class);
    }

    // ─── once the match has started its data is fixed ────────────────────────────

    private static void assertRefusedAsAlreadyStarted(Runnable update, String field) {
        assertThatThrownBy(update::run)
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry(field, MatchError.MATCH_ALREADY_STARTED.name()));
    }

    @Test
    void update_whenAStartedMatchChangesATeam_thenRefusedOnThatField() {
        MatchesEntity started = storedMatch(LocalDateTime.now().minusMinutes(10), "SCHEDULED");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setHomeTeamId(UUID.randomUUID());

        assertRefusedAsAlreadyStarted(() -> matchesService.update(started.getId(), request), "homeTeamId");
        verify(matchRepository, never()).save(any());
    }

    @Test
    void update_whenAStartedMatchChangesItsDate_itsPlaceOrItsCourt_thenRefusedOnThatField() {
        MatchesEntity started = storedMatch(LocalDateTime.now().minusMinutes(10), "IN_PROGRESS");

        UpdateMatchRequest newDate = new UpdateMatchRequest();
        newDate.setMatchDate(LocalDateTime.now().plusDays(3));
        assertRefusedAsAlreadyStarted(() -> matchesService.update(started.getId(), newDate), "matchDate");

        UpdateMatchRequest newPlace = new UpdateMatchRequest();
        newPlace.setLocation("Otro estadio");
        assertRefusedAsAlreadyStarted(() -> matchesService.update(started.getId(), newPlace), "location");

        UpdateMatchRequest newCourt = new UpdateMatchRequest();
        newCourt.setCourtId(UUID.randomUUID());
        assertRefusedAsAlreadyStarted(() -> matchesService.update(started.getId(), newCourt), "courtId");
    }

    @Test
    void update_whenAStartedMatchSendsItsDataUnchangedAlongWithAStatus_thenOnlyTheStatusChanges() {
        // the mobile app sends every field on each save: the ones that did not change must not be a problem
        MatchesEntity started = storedMatch(LocalDateTime.now().minusMinutes(10), "SCHEDULED");
        started.setLocation("Estadio Norte");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setHomeTeamId(started.getHomeTeamId());
        request.setAwayTeamId(started.getAwayTeamId());
        request.setMatchDate(started.getMatchDate());
        request.setLocation("Estadio Norte");
        request.setEventId(started.getEventId());
        request.setStatus("IN_PROGRESS");

        MatchResponse response = matchesService.update(started.getId(), request);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getLocation()).isEqualTo("Estadio Norte");
    }

    @Test
    void update_whenAStartedMatchOnlyChangesItsStatus_thenAccepted() {
        MatchesEntity started = storedMatch(LocalDateTime.now().minusMinutes(10), "SCHEDULED");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setStatus("SUSPENDED");

        assertThat(matchesService.update(started.getId(), request).getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void update_whenAnEndedMatchOnlyGetsItsResult_thenAccepted() {
        MatchesEntity ended = storedMatch(hoursAgo(3), "SCHEDULED");

        MatchResponse response = matchesService.update(ended.getId(), scores(3, 2));

        assertThat(response.getHomeScore()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo("FINISHED");
    }

    @Test
    void update_whenAMatchHasNotStartedYet_thenItsDataCanChange() {
        MatchesEntity upcoming = storedMatch(LocalDateTime.now().plusHours(2), "SCHEDULED");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setHomeTeamId(UUID.randomUUID());
        request.setMatchDate(LocalDateTime.now().plusDays(1));
        request.setLocation("Estadio Sur");

        MatchResponse response = matchesService.update(upcoming.getId(), request);

        assertThat(response.getHomeTeamId()).isEqualTo(request.getHomeTeamId());
        assertThat(response.getLocation()).isEqualTo("Estadio Sur");
    }

    @Test
    void update_whenTheMatchStartsRightNow_thenItsDataIsAlreadyFixed() {
        // from the very kick-off, not after it
        MatchesEntity startingNow = storedMatch(LocalDateTime.now().minusSeconds(1), "SCHEDULED");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setLocation("Otro estadio");

        assertRefusedAsAlreadyStarted(() -> matchesService.update(startingNow.getId(), request), "location");
    }

    @Test
    void update_whenAPostponedMatchPassedItsOldDate_thenItsDataCanStillBeCorrected() {
        // it was never played: until it is rescheduled its data is not fixed
        MatchesEntity postponed = storedMatch(hoursAgo(5), "POSTPONED");
        UpdateMatchRequest request = new UpdateMatchRequest();
        request.setHomeTeamId(UUID.randomUUID());

        assertThat(matchesService.update(postponed.getId(), request).getHomeTeamId()).isEqualTo(request.getHomeTeamId());
    }

    // ─── responses ──────────────────────────────────────────────────────────────

    @Test
    void getByEventId_whenCalled_thenEveryMatchCarriesWhenItEnds() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime kickOff = LocalDateTime.of(2026, 10, 17, 10, 0);
        MatchesEntity stored = match(kickOff, "SCHEDULED");
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(stored));

        List<MatchResponse> responses = matchesService.getByEventId(eventId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEndsAt()).isEqualTo(kickOff.plusMinutes(SLOT_MINUTES));
    }

    // ─── finishing ended matches by themselves ──────────────────────────────────

    @Test
    void finishEndedMatches_whenTheirTimeIsOver_thenOnlyThoseAreFinished() {
        MatchesEntity ended = match(hoursAgo(3), "SCHEDULED");
        MatchesEntity stillInItsSlot = match(LocalDateTime.now().minusMinutes(20), "IN_PROGRESS");
        MatchesEntity endedRescheduled = match(hoursAgo(2), "RESCHEDULED");
        when(matchRepository.findAllActiveByStatusInAndStartedBy(any(), any()))
                .thenReturn(List.of(ended, stillInItsSlot, endedRescheduled));

        int finished = matchesService.finishEndedMatches();

        assertThat(finished).isEqualTo(2);
        assertThat(ended.getStatus()).isEqualTo("FINISHED");
        assertThat(endedRescheduled.getStatus()).isEqualTo("FINISHED");
        assertThat(stillInItsSlot.getStatus()).isEqualTo("IN_PROGRESS");
        verify(matchRepository).save(ended);
        verify(matchRepository).save(endedRescheduled);
        verify(matchRepository, never()).save(stillInItsSlot);
    }

    @Test
    void finishEndedMatches_whenAskingForTheStartedMatches_thenOnlyTheAutoFinishableStatusesAreRequested() {
        when(matchRepository.findAllActiveByStatusInAndStartedBy(any(), any())).thenReturn(List.of());

        matchesService.finishEndedMatches();

        verify(matchRepository).findAllActiveByStatusInAndStartedBy(
                org.mockito.ArgumentMatchers.argThat(statuses -> statuses.containsAll(List.of("SCHEDULED", "RESCHEDULED", "IN_PROGRESS"))
                        && statuses.size() == 3),
                any());
    }

    @Test
    void finishEndedMatches_whenNothingEnded_thenNothingIsSaved() {
        when(matchRepository.findAllActiveByStatusInAndStartedBy(any(), any()))
                .thenReturn(List.of(match(LocalDateTime.now().minusMinutes(5), "SCHEDULED")));

        assertThat(matchesService.finishEndedMatches()).isZero();
        verify(matchRepository, never()).save(any());
    }

    @Test
    void finishEndedMatches_forOneEvent_thenOnlyItsOpenMatchesWhoseTimeIsOverAreFinished() {
        UUID eventId = UUID.randomUUID();
        MatchesEntity ended = match(hoursAgo(3), "SCHEDULED");
        MatchesEntity postponed = match(hoursAgo(3), "POSTPONED");
        MatchesEntity cancelled = match(hoursAgo(3), "CANCELLED");
        MatchesEntity alreadyFinished = match(hoursAgo(3), "FINISHED");
        MatchesEntity upcoming = match(LocalDateTime.now().plusDays(1), "SCHEDULED");
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(ended, postponed, cancelled, alreadyFinished, upcoming));

        int finished = matchesService.finishEndedMatches(eventId);

        assertThat(finished).isEqualTo(1);
        assertThat(ended.getStatus()).isEqualTo("FINISHED");
        assertThat(postponed.getStatus()).isEqualTo("POSTPONED");
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(upcoming.getStatus()).isEqualTo("SCHEDULED");
    }
}
