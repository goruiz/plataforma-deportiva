package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchDisputeResponseAssembler;
import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.DisputeStatus;
import com.platform.backend.modules.matches.domain.exception.DisputeError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchDisputeRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.OpenDisputeRequest.OpenDisputeRequest;
import com.platform.backend.modules.matches.presentation.requests.ResolveDisputeRequest.ResolveDisputeRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchDisputeResponse.MatchDisputeResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.modules.users.application.iservices.IAccountCapabilitiesService;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.enums.AccountCapability;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchDisputesServiceTest {

    private static final UUID MATCH_ID = UUID.randomUUID();
    private static final UUID HOME_TEAM_ID = UUID.randomUUID();
    private static final UUID AWAY_TEAM_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final String CALLER_EMAIL = "capitan@equipo.com";
    private static final int WINDOW_HOURS = 48;

    @Mock private IMatchDisputeRepository disputeRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private IMatchesService matchesService;
    @Mock private MatchSettingsReader settingsReader;
    @Mock private IUserRepository userRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamMemberRepository teamMemberRepository;
    @Mock private IAccountCapabilitiesService accountCapabilities;
    @Mock private MatchDisputeResponseAssembler assembler;

    @InjectMocks
    private MatchDisputesService service;

    private MatchesEntity match;

    @BeforeEach
    void setUp() {
        match = new MatchesEntity();
        match.setId(MATCH_ID);
        match.setEventId(EVENT_ID);
        match.setHomeTeamId(HOME_TEAM_ID);
        match.setAwayTeamId(AWAY_TEAM_ID);
        match.setHomeScore(2);
        match.setAwayScore(1);
        match.setResultRecordedAt(LocalDateTime.now().minusHours(1));
        lenient().when(matchRepository.findActiveById(MATCH_ID)).thenReturn(Optional.of(match));
        lenient().when(settingsReader.of(any())).thenReturn(new SettingsValues(15, 5, true, 0, false, WINDOW_HOURS));
        lenient().when(disputeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(assembler.toResponses(any())).thenAnswer(invocation -> List.of(new MatchDisputeResponse()));
        UsersEntity caller = new UsersEntity();
        caller.setFirstName("Carla");
        caller.setLastName("Ruiz");
        caller.setEmail(CALLER_EMAIL);
        lenient().when(userRepository.findActiveById(any())).thenReturn(Optional.of(caller));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private void actAsAdministrator() {
        TenantTestSupport.actAsOrganizationAdmin(CALLER_ID, UUID.randomUUID());
    }

    /** An account of the app that plays for {@code teamId}; whether it is a «dirigente de equipo» is what the tests vary. */
    private void actAsPlayerOf(UUID teamId, boolean isTeamManager) {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, CALLER_EMAIL);
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        lenient().when(playerRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(player));
        lenient().when(teamMemberRepository.findActiveByPlayerIdAndTeamId(player.getId(), teamId))
                .thenReturn(Optional.of(mock(TeamMembersEntity.class)));
        lenient().when(accountCapabilities.has(CALLER_ID, AccountCapability.OPEN_MATCH_DISPUTES)).thenReturn(isTeamManager);
    }

    private void actAsTeamManagerOf(UUID teamId) {
        actAsPlayerOf(teamId, true);
    }

    private static OpenDisputeRequest dispute(UUID teamId, String reason) {
        OpenDisputeRequest request = new OpenDisputeRequest();
        request.setTeamId(teamId);
        request.setReason(reason);
        return request;
    }

    private MatchDisputesEntity openDisputeInStore() {
        MatchDisputesEntity stored = new MatchDisputesEntity();
        stored.setId(UUID.randomUUID());
        stored.setMatchId(MATCH_ID);
        stored.setTeamId(HOME_TEAM_ID);
        stored.setReason("El marcador está al revés");
        when(disputeRepository.findActiveById(stored.getId())).thenReturn(Optional.of(stored));
        return stored;
    }

    private static ResolveDisputeRequest decision(boolean accepted, String note, Integer home, Integer away) {
        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setAccepted(accepted);
        request.setNote(note);
        request.setHomeScore(home);
        request.setAwayScore(away);
        return request;
    }

    // ─── opening ─────────────────────────────────────────────────────────────────

    @Test
    void aTeamManagerOfATeamOfTheMatchOpensADisputeWithTheirNameAndATrimmedReason() {
        actAsTeamManagerOf(HOME_TEAM_ID);

        service.open(MATCH_ID, dispute(HOME_TEAM_ID, "  El gol del minuto 80 no fue nuestro  "));

        ArgumentCaptor<MatchDisputesEntity> saved = ArgumentCaptor.forClass(MatchDisputesEntity.class);
        verify(disputeRepository).save(saved.capture());
        assertThat(saved.getValue().getMatchId()).isEqualTo(MATCH_ID);
        assertThat(saved.getValue().getTeamId()).isEqualTo(HOME_TEAM_ID);
        assertThat(saved.getValue().getReason()).isEqualTo("El gol del minuto 80 no fue nuestro");
        assertThat(saved.getValue().getOpenedByName()).isEqualTo("Carla Ruiz");
        assertThat(saved.getValue().getStatus()).isEqualTo(DisputeStatus.OPEN);
    }

    @Test
    void aTeamThatDoesNotPlayTheMatchCannotDisputeIt() {
        actAsAdministrator();

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(UUID.randomUUID(), "x")))
                .isInstanceOfSatisfying(FieldValidationException.class, refused ->
                        assertThat(refused.getFieldErrors()).containsEntry("teamId", DisputeError.TEAM_NOT_IN_MATCH.name()));
    }

    @Test
    void aPlayerWhoIsNotATeamManagerCannotDisputeForTheTeamNotEvenAsItsCaptain() {
        actAsPlayerOf(HOME_TEAM_ID, false);

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(HOME_TEAM_ID, "x")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(DisputeError.NOT_ALLOWED_FOR_TEAM.name());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void aTeamManagerCannotDisputeForTheOtherTeam() {
        actAsTeamManagerOf(HOME_TEAM_ID);

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(AWAY_TEAM_ID, "x")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(DisputeError.NOT_ALLOWED_FOR_TEAM.name());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void anAccountWithoutAPlayerCannotDisputeEvenIfItHoldsTheCapability() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, CALLER_EMAIL);
        when(accountCapabilities.has(CALLER_ID, AccountCapability.OPEN_MATCH_DISPUTES)).thenReturn(true);
        when(playerRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(HOME_TEAM_ID, "x")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aMatchWithoutAResultCannotBeDisputed() {
        actAsAdministrator();
        match.setHomeScore(null);
        match.setAwayScore(null);

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(HOME_TEAM_ID, "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DisputeError.RESULT_NOT_RECORDED.name());
    }

    @Test
    void aTeamManagerCannotDisputeOnceTheWindowIsOver() {
        actAsTeamManagerOf(HOME_TEAM_ID);
        match.setResultRecordedAt(LocalDateTime.now().minusHours(WINDOW_HOURS + 1));

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(HOME_TEAM_ID, "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DisputeError.WINDOW_CLOSED.name());
    }

    @Test
    void anAdministratorMayDisputeOnATeamsBehalfAfterTheWindow() {
        actAsAdministrator();
        match.setResultRecordedAt(LocalDateTime.now().minusDays(30));

        service.open(MATCH_ID, dispute(AWAY_TEAM_ID, "Lo pidió el equipo por teléfono"));

        verify(disputeRepository).save(any());
    }

    @Test
    void aTeamCannotHaveTwoOpenDisputesAgainstTheSameMatch() {
        actAsAdministrator();
        when(disputeRepository.existsOpenByMatchIdAndTeamId(MATCH_ID, HOME_TEAM_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(HOME_TEAM_ID, "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DisputeError.ALREADY_OPEN.name());
    }

    @Test
    void aDisputeNeedsAReason() {
        actAsAdministrator();

        assertThatThrownBy(() -> service.open(MATCH_ID, dispute(HOME_TEAM_ID, "   ")))
                .isInstanceOfSatisfying(FieldValidationException.class, refused ->
                        assertThat(refused.getFieldErrors()).containsEntry("reason", DisputeError.REASON_REQUIRED.name()));
    }

    @Test
    void aProposedScoreNeedsBothGoals() {
        actAsAdministrator();
        OpenDisputeRequest request = dispute(HOME_TEAM_ID, "x");
        request.setProposedHomeScore(3);

        assertThatThrownBy(() -> service.open(MATCH_ID, request))
                .isInstanceOfSatisfying(FieldValidationException.class, refused ->
                        assertThat(refused.getFieldErrors()).containsEntry("proposedHomeScore", DisputeError.SCORE_INCOMPLETE.name()));
    }

    @Test
    void openingADisputeTakesTheValidationAwayFromTheResult() {
        actAsAdministrator();
        match.setResultValidatedAt(LocalDateTime.now().minusHours(3));
        match.setResultValidatedBy(UUID.randomUUID());

        service.open(MATCH_ID, dispute(HOME_TEAM_ID, "x"));

        assertThat(match.getResultValidatedAt()).isNull();
        assertThat(match.getResultValidatedBy()).isNull();
        verify(matchRepository).save(match);
    }

    // ─── resolving ───────────────────────────────────────────────────────────────

    @Test
    void anAdministratorRejectingNeedsToSayWhy() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();

        assertThatThrownBy(() -> service.resolve(stored.getId(), decision(false, " ", null, null)))
                .isInstanceOfSatisfying(FieldValidationException.class, refused ->
                        assertThat(refused.getFieldErrors()).containsEntry("note", DisputeError.NOTE_REQUIRED.name()));
        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.OPEN);
    }

    @Test
    void rejectingKeepsTheResultAndRecordsWhoDecidedAndWhy() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();

        service.resolve(stored.getId(), decision(false, "El árbitro firmó el acta", null, null));

        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.REJECTED);
        assertThat(stored.getResolutionNote()).isEqualTo("El árbitro firmó el acta");
        assertThat(stored.getResolvedBy()).isEqualTo(CALLER_ID);
        assertThat(stored.getResolvedByName()).isEqualTo("Carla Ruiz");
        assertThat(stored.getResolvedAt()).isNotNull();
        verify(matchesService, never()).update(any(), any());
    }

    @Test
    void acceptingWithACorrectedResultAppliesItThroughTheMatchUpdate() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();
        ResolveDisputeRequest request = decision(true, null, 1, 1);
        request.setHomePenaltyScore(4);
        request.setAwayPenaltyScore(3);

        service.resolve(stored.getId(), request);

        ArgumentCaptor<UpdateMatchRequest> correction = ArgumentCaptor.forClass(UpdateMatchRequest.class);
        verify(matchesService).update(eq(MATCH_ID), correction.capture());
        assertThat(correction.getValue().getHomeScore()).isEqualTo(1);
        assertThat(correction.getValue().getAwayScore()).isEqualTo(1);
        assertThat(correction.getValue().getHomePenaltyScore()).isEqualTo(4);
        assertThat(correction.getValue().getAwayPenaltyScore()).isEqualTo(3);
        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.ACCEPTED);
    }

    @Test
    void acceptingWithoutAResultLeavesTheMatchAsItIs() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();

        service.resolve(stored.getId(), decision(true, "Se corrige en el acta", null, null));

        verify(matchesService, never()).update(any(), any());
        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.ACCEPTED);
    }

    @Test
    void aCorrectedResultNeedsBothGoals() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();

        assertThatThrownBy(() -> service.resolve(stored.getId(), decision(true, null, 2, null)))
                .isInstanceOfSatisfying(FieldValidationException.class, refused ->
                        assertThat(refused.getFieldErrors()).containsEntry("homeScore", DisputeError.SCORE_INCOMPLETE.name()));
        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.OPEN);
    }

    @Test
    void whenTheMatchRefusesTheCorrectionTheDisputeStaysOpen() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();
        when(matchesService.update(eq(MATCH_ID), any()))
                .thenThrow(new FieldValidationException("homeScore", "DRAW_NOT_ALLOWED"));

        assertThatThrownBy(() -> service.resolve(stored.getId(), decision(true, null, 1, 1)))
                .isInstanceOf(FieldValidationException.class);

        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.OPEN);
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void aDisputeThatWasAlreadyResolvedCannotBeResolvedAgain() {
        actAsAdministrator();
        MatchDisputesEntity stored = openDisputeInStore();
        stored.setStatus(DisputeStatus.REJECTED);

        assertThatThrownBy(() -> service.resolve(stored.getId(), decision(false, "x", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DisputeError.NOT_OPEN.name());
    }

    @Test
    void anAccountThatIsNeitherAnAdministratorNorTheCreatorOfTheEventCannotResolve() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, CALLER_EMAIL);
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.of(new EventsEntity()));
        MatchDisputesEntity stored = openDisputeInStore();

        assertThatThrownBy(() -> service.resolve(stored.getId(), decision(false, "x", null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void theCreatorOfTheEventMayResolve() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, CALLER_EMAIL);
        EventsEntity event = new EventsEntity();
        event.setCreatedBy(CALLER_ID);
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.of(event));
        MatchDisputesEntity stored = openDisputeInStore();

        service.resolve(stored.getId(), decision(false, "No hay pruebas", null, null));

        assertThat(stored.getStatus()).isEqualTo(DisputeStatus.REJECTED);
    }

    // ─── reading ─────────────────────────────────────────────────────────────────

    @Test
    void aPlayerWhoIsNotATeamManagerSeesNoDisputesAtAll() {
        actAsPlayerOf(HOME_TEAM_ID, false);

        assertThat(service.getMine()).isEmpty();
        service.getByMatch(MATCH_ID);

        ArgumentCaptor<List<MatchDisputesEntity>> shown = ArgumentCaptor.forClass(List.class);
        // getMine() answers at once with nothing; only the match's list went through the assembler, and it is empty.
        verify(assembler).toResponses(shown.capture());
        assertThat(shown.getValue()).isEmpty();
        verify(disputeRepository, never()).findAllActiveByTeamIds(any());
    }

    @Test
    void aTeamManagerOnlySeesTheDisputesOfTheirOwnTeamsForAMatch() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, CALLER_EMAIL);
        when(accountCapabilities.has(CALLER_ID, AccountCapability.OPEN_MATCH_DISPUTES)).thenReturn(true);
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        when(playerRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(player));
        com.platform.backend.modules.teams.domain.entities.TeamsEntity ownTeam =
                new com.platform.backend.modules.teams.domain.entities.TeamsEntity();
        ownTeam.setId(HOME_TEAM_ID);
        TeamMembersEntity membership = mock(TeamMembersEntity.class);
        when(membership.getTeam()).thenReturn(ownTeam);
        when(teamMemberRepository.findAllActiveByPlayerId(player.getId())).thenReturn(List.of(membership));
        MatchDisputesEntity own = new MatchDisputesEntity();
        own.setTeamId(HOME_TEAM_ID);
        MatchDisputesEntity theirs = new MatchDisputesEntity();
        theirs.setTeamId(AWAY_TEAM_ID);
        when(disputeRepository.findAllActiveByMatchId(MATCH_ID)).thenReturn(List.of(own, theirs));

        service.getByMatch(MATCH_ID);

        ArgumentCaptor<List<MatchDisputesEntity>> shown = ArgumentCaptor.forClass(List.class);
        verify(assembler).toResponses(shown.capture());
        assertThat(shown.getValue()).containsExactly(own);
    }
}
