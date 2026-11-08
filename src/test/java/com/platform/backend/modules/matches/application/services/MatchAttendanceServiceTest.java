package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.domain.entities.MatchAttendancesEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.AttendanceStatus;
import com.platform.backend.modules.matches.domain.exception.MatchAttendanceError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchAttendanceRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.UpdateAttendanceRequest.UpdateAttendanceRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchAttendanceResponse.MatchAttendanceResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchAttendanceServiceTest {

    private static final String EMAIL = "Juan@Email.com";

    @Mock private IMatchRepository matchRepository;
    @Mock private IMatchAttendanceRepository attendanceRepository;
    @Mock private IPlayerRepository playerRepository;

    @InjectMocks
    private MatchAttendanceService matchAttendanceService;

    private final TeamsEntity tigres = team();
    private final TeamsEntity leones = team();
    private PlayersEntity juan;
    private MatchesEntity match;
    private final List<MatchAttendancesEntity> storedAnswers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        juan = player("juan@email.com", tigres);
        match = matchBetween(tigres, leones, LocalDateTime.now().plusDays(2), "SCHEDULED");
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), EMAIL);

        lenient().when(matchRepository.findActiveById(match.getId())).thenReturn(Optional.of(match));
        lenient().when(playerRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(juan));
        lenient().when(attendanceRepository.findAllActiveByMatchId(match.getId())).thenReturn(storedAnswers);
        lenient().when(attendanceRepository.save(any())).thenAnswer(invocation -> {
            MatchAttendancesEntity saved = invocation.getArgument(0);
            if (!storedAnswers.contains(saved)) {
                storedAnswers.add(saved);
            }
            return saved;
        });
        lenient().when(playerRepository.findFullNamesByIds(any())).thenReturn(Map.of());
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

    private static PlayersEntity player(String email, TeamsEntity team) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(email);
        player.setTeam(team);
        return player;
    }

    private static MatchesEntity matchBetween(TeamsEntity home, TeamsEntity away, LocalDateTime kickOff, String status) {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setHomeTeamId(home.getId());
        match.setAwayTeamId(away.getId());
        match.setMatchDate(kickOff);
        match.setStatus(status);
        return match;
    }

    private static UpdateAttendanceRequest answer(String status) {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(status);
        return request;
    }

    private MatchAttendancesEntity answerOf(PlayersEntity player, TeamsEntity team, AttendanceStatus status) {
        MatchAttendancesEntity answer = new MatchAttendancesEntity();
        answer.setMatchId(match.getId());
        answer.setPlayerId(player.getId());
        answer.setTeamId(team.getId());
        answer.setStatus(status.name());
        storedAnswers.add(answer);
        return answer;
    }

    // ─── respond ────────────────────────────────────────────────────────────────

    @Test
    void respond_whenAPlayerOfTheHomeTeamConfirms_thenTheAnswerIsStoredForTheirTeam() {
        when(attendanceRepository.findActiveByMatchIdAndPlayerId(match.getId(), juan.getId())).thenReturn(Optional.empty());

        MatchAttendanceResponse response = matchAttendanceService.respond(match.getId(), answer("CONFIRMED"));

        ArgumentCaptor<MatchAttendancesEntity> saved = ArgumentCaptor.forClass(MatchAttendancesEntity.class);
        verify(attendanceRepository).save(saved.capture());
        assertThat(saved.getValue().getMatchId()).isEqualTo(match.getId());
        assertThat(saved.getValue().getPlayerId()).isEqualTo(juan.getId());
        assertThat(saved.getValue().getTeamId()).isEqualTo(tigres.getId());
        assertThat(saved.getValue().getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getMyStatus()).isEqualTo(AttendanceStatus.CONFIRMED);
        assertThat(response.getConfirmedCount()).isEqualTo(1);
        assertThat(response.getDeclinedCount()).isZero();
    }

    @Test
    void respond_whenAPlayerOfTheAwayTeamDeclines_thenTheAnswerIsStoredForTheirTeam() {
        PlayersEntity ana = player("ana@email.com", leones);
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), "ana@email.com");
        when(playerRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana));
        when(attendanceRepository.findActiveByMatchIdAndPlayerId(match.getId(), ana.getId())).thenReturn(Optional.empty());

        MatchAttendanceResponse response = matchAttendanceService.respond(match.getId(), answer("DECLINED"));

        assertThat(storedAnswers).singleElement().satisfies(stored -> assertThat(stored.getTeamId()).isEqualTo(leones.getId()));
        assertThat(response.getMyStatus()).isEqualTo(AttendanceStatus.DECLINED);
        assertThat(response.getDeclinedCount()).isEqualTo(1);
    }

    @Test
    void respond_whenThePlayerAlreadyAnswered_thenTheSameAnswerChangesAndNoSecondOneAppears() {
        MatchAttendancesEntity before = answerOf(juan, tigres, AttendanceStatus.CONFIRMED);
        when(attendanceRepository.findActiveByMatchIdAndPlayerId(match.getId(), juan.getId())).thenReturn(Optional.of(before));

        MatchAttendanceResponse response = matchAttendanceService.respond(match.getId(), answer("DECLINED"));

        assertThat(before.getStatus()).isEqualTo("DECLINED");
        assertThat(storedAnswers).hasSize(1);
        assertThat(response.getConfirmedCount()).isZero();
        assertThat(response.getDeclinedCount()).isEqualTo(1);
        assertThat(response.getMyStatus()).isEqualTo(AttendanceStatus.DECLINED);
    }

    @Test
    void respond_whenTheMatchWasRescheduledAndStillLiesAhead_thenItCanBeAnswered() {
        match.setStatus("RESCHEDULED");
        when(attendanceRepository.findActiveByMatchIdAndPlayerId(match.getId(), juan.getId())).thenReturn(Optional.empty());

        matchAttendanceService.respond(match.getId(), answer("CONFIRMED"));

        verify(attendanceRepository).save(any());
    }

    @Test
    void respond_whenTheStatusIsNotKnown_thenItIsAFieldErrorOnStatus() {
        assertThatThrownBy(() -> matchAttendanceService.respond(match.getId(), answer("MAYBE")))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("status", MatchAttendanceError.INVALID_STATUS.name())));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void respond_whenThePlayerBelongsToAnotherTeam_thenNotAParticipant() {
        juan.setTeam(team());

        assertThatThrownBy(() -> matchAttendanceService.respond(match.getId(), answer("CONFIRMED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MatchAttendanceError.NOT_A_PARTICIPANT.name());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void respond_whenThePlayerHasNoTeam_thenNotAParticipant() {
        juan.setTeam(null);

        assertThatThrownBy(() -> matchAttendanceService.respond(match.getId(), answer("CONFIRMED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MatchAttendanceError.NOT_A_PARTICIPANT.name());
    }

    @Test
    void respond_whenTheMatchAlreadyStarted_thenMatchNotOpen() {
        match.setMatchDate(LocalDateTime.now().minusMinutes(5));

        assertThatThrownBy(() -> matchAttendanceService.respond(match.getId(), answer("CONFIRMED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MatchAttendanceError.MATCH_NOT_OPEN.name());
        verify(attendanceRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"IN_PROGRESS", "FINISHED", "COMPLETED", "CANCELLED", "POSTPONED", "SUSPENDED"})
    void respond_whenTheMatchIsOverCalledOffOrPutOff_thenMatchNotOpen(String status) {
        match.setStatus(status);

        assertThatThrownBy(() -> matchAttendanceService.respond(match.getId(), answer("CONFIRMED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MatchAttendanceError.MATCH_NOT_OPEN.name());
    }

    @Test
    void respond_whenTheAccountIsNotAPlayer_thenNotFound() {
        when(playerRepository.findByEmail("juan@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAttendanceService.respond(match.getId(), answer("CONFIRMED")))
                .isInstanceOf(EntityNotFoundException.class);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void respond_whenTheMatchIsNotVisibleToTheCaller_thenNotFound() {
        UUID unknownMatch = UUID.randomUUID();
        when(matchRepository.findActiveById(unknownMatch)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAttendanceService.respond(unknownMatch, answer("CONFIRMED")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── withdraw ───────────────────────────────────────────────────────────────

    @Test
    void withdraw_whenThePlayerHadAnswered_thenTheAnswerIsSoftDeletedAndNoLongerCounts() {
        MatchAttendancesEntity before = answerOf(juan, tigres, AttendanceStatus.CONFIRMED);
        when(attendanceRepository.findActiveByMatchIdAndPlayerId(match.getId(), juan.getId())).thenReturn(Optional.of(before));
        lenient().doAnswer(invocation -> storedAnswers.remove(before)).when(attendanceRepository).delete(before);

        MatchAttendanceResponse response = matchAttendanceService.withdraw(match.getId());

        assertThat(before.getDeletedAt()).isNotNull();
        verify(attendanceRepository).delete(before);
        assertThat(response.getMyStatus()).isNull();
        assertThat(response.getConfirmedCount()).isZero();
    }

    @Test
    void withdraw_whenThePlayerHadNotAnswered_thenNothingChanges() {
        when(attendanceRepository.findActiveByMatchIdAndPlayerId(match.getId(), juan.getId())).thenReturn(Optional.empty());

        MatchAttendanceResponse response = matchAttendanceService.withdraw(match.getId());

        verify(attendanceRepository, never()).delete(any());
        assertThat(response.getMyStatus()).isNull();
    }

    @Test
    void withdraw_whenTheAccountIsNotAPlayer_thenNotFound() {
        when(playerRepository.findByEmail("juan@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAttendanceService.withdraw(match.getId())).isInstanceOf(EntityNotFoundException.class);
    }

    // ─── read ───────────────────────────────────────────────────────────────────

    @Test
    void getByMatchId_whenAPlayerOfTheMatchAsks_thenTheyGetTheTotalsTheirOwnAnswerAndTheNames() {
        PlayersEntity ana = player("ana@email.com", leones);
        PlayersEntity luis = player("luis@email.com", tigres);
        answerOf(juan, tigres, AttendanceStatus.CONFIRMED);
        answerOf(ana, leones, AttendanceStatus.DECLINED);
        answerOf(luis, tigres, AttendanceStatus.CONFIRMED);
        when(playerRepository.findFullNamesByIds(any())).thenReturn(Map.of(
                juan.getId(), "Juan Perez", ana.getId(), "Ana Lopez", luis.getId(), "Luis Mora"));

        MatchAttendanceResponse response = matchAttendanceService.getByMatchId(match.getId());

        assertThat(response.getConfirmedCount()).isEqualTo(2);
        assertThat(response.getDeclinedCount()).isEqualTo(1);
        assertThat(response.getMyStatus()).isEqualTo(AttendanceStatus.CONFIRMED);
        // Confirmed first, then by name.
        assertThat(response.getAttendees()).extracting("playerName", "status")
                .containsExactly(
                        tuple("Juan Perez", AttendanceStatus.CONFIRMED),
                        tuple("Luis Mora", AttendanceStatus.CONFIRMED),
                        tuple("Ana Lopez", AttendanceStatus.DECLINED));
        assertThat(response.getAttendees()).first().satisfies(first -> assertThat(first.getTeamId()).isEqualTo(tigres.getId()));
    }

    @Test
    void getByMatchId_whenAPlayerOfAnotherTeamLooksAtAPublicMatch_thenTheyGetTheTotalsButNoNames() {
        juan.setTeam(team());
        answerOf(player("ana@email.com", leones), leones, AttendanceStatus.CONFIRMED);

        MatchAttendanceResponse response = matchAttendanceService.getByMatchId(match.getId());

        assertThat(response.getConfirmedCount()).isEqualTo(1);
        assertThat(response.getAttendees()).isEmpty();
        assertThat(response.getMyStatus()).isNull();
        verify(playerRepository, never()).findFullNamesByIds(any());
    }

    @Test
    void getByMatchId_whenAPlayerWithoutTeamAsks_thenTheyGetTheTotalsButNoNames() {
        juan.setTeam(null);
        answerOf(player("ana@email.com", leones), leones, AttendanceStatus.DECLINED);

        MatchAttendanceResponse response = matchAttendanceService.getByMatchId(match.getId());

        assertThat(response.getDeclinedCount()).isEqualTo(1);
        assertThat(response.getAttendees()).isEmpty();
    }

    @Test
    void getByMatchId_whenAnAdministratorAsks_thenTheyGetTheNamesAndNoOwnAnswer() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        when(playerRepository.findByEmail("ana@liga.com")).thenReturn(Optional.empty());
        PlayersEntity ana = player("ana@email.com", leones);
        answerOf(ana, leones, AttendanceStatus.CONFIRMED);
        when(playerRepository.findFullNamesByIds(any())).thenReturn(Map.of(ana.getId(), "Ana Lopez"));

        MatchAttendanceResponse response = matchAttendanceService.getByMatchId(match.getId());

        assertThat(response.getMyStatus()).isNull();
        assertThat(response.getAttendees()).singleElement().satisfies(attendee -> {
            assertThat(attendee.getPlayerName()).isEqualTo("Ana Lopez");
            assertThat(attendee.getStatus()).isEqualTo(AttendanceStatus.CONFIRMED);
        });
    }

    @Test
    void getByMatchId_whenNobodyAnsweredYet_thenEverythingIsEmpty() {
        MatchAttendanceResponse response = matchAttendanceService.getByMatchId(match.getId());

        assertThat(response.getMatchId()).isEqualTo(match.getId());
        assertThat(response.getConfirmedCount()).isZero();
        assertThat(response.getDeclinedCount()).isZero();
        assertThat(response.getMyStatus()).isNull();
        assertThat(response.getAttendees()).isEmpty();
    }

    @Test
    void getByMatchId_whenTheMatchIsNotVisibleToTheCaller_thenNotFound() {
        UUID unknownMatch = UUID.randomUUID();
        when(matchRepository.findActiveById(unknownMatch)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAttendanceService.getByMatchId(unknownMatch)).isInstanceOf(EntityNotFoundException.class);
    }
}
