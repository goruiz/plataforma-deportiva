package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchAttendanceService;
import com.platform.backend.modules.matches.domain.entities.MatchAttendancesEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.AttendanceStatus;
import com.platform.backend.modules.matches.domain.enums.MatchStatus;
import com.platform.backend.modules.matches.domain.exception.MatchAttendanceError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchAttendanceRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.UpdateAttendanceRequest.UpdateAttendanceRequest;
import com.platform.backend.modules.matches.presentation.responses.AttendeeResponse.AttendeeResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchAttendanceResponse.MatchAttendanceResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A match is visible to the callers its organization (or its public event) shows it to, and its answers hang from it:
 * a match the caller cannot see is a 404, and so are its answers. Only an administrator and the players of the two
 * teams get the names; everyone else who sees the match gets the totals.
 */
@Service
@RequiredArgsConstructor
public class MatchAttendanceService implements IMatchAttendanceService {

    private static final String STATUS_FIELD = "status";

    // Confirmed first, then by name: the order a captain reads the list in.
    private static final Comparator<AttendeeResponse> ATTENDEE_ORDER =
            Comparator.comparing(AttendeeResponse::getStatus)
                    .thenComparing(AttendeeResponse::getPlayerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final IMatchRepository matchRepository;
    private final IMatchAttendanceRepository attendanceRepository;
    private final IPlayerRepository playerRepository;
    private final ITeamMemberRepository teamMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public MatchAttendanceResponse getByMatchId(UUID matchId) {
        MatchesEntity match = findActiveMatchOrThrow(matchId);
        return toResponse(match, findCurrentPlayer());
    }

    @Override
    @Transactional
    public MatchAttendanceResponse respond(UUID matchId, UpdateAttendanceRequest request) {
        PlayersEntity player = requireCurrentPlayer();
        MatchesEntity match = findActiveMatchOrThrow(matchId);
        AttendanceStatus status = requireValidStatus(request.getStatus());
        UUID teamId = requireTeamOfTheMatch(player, match);
        requireMatchStillOpen(match);

        MatchAttendancesEntity attendance = attendanceRepository.findActiveByMatchIdAndPlayerId(matchId, player.getId())
                .orElseGet(() -> newAttendance(matchId, player.getId()));
        attendance.setTeamId(teamId);
        attendance.setStatus(status.name());
        attendanceRepository.save(attendance);
        return toResponse(match, Optional.of(player));
    }

    @Override
    @Transactional
    public MatchAttendanceResponse withdraw(UUID matchId) {
        PlayersEntity player = requireCurrentPlayer();
        MatchesEntity match = findActiveMatchOrThrow(matchId);

        attendanceRepository.findActiveByMatchIdAndPlayerId(matchId, player.getId()).ifPresent(attendance -> {
            attendance.setDeletedAt(LocalDateTime.now());
            attendanceRepository.delete(attendance);
        });
        return toResponse(match, Optional.of(player));
    }

    private MatchesEntity findActiveMatchOrThrow(UUID matchId) {
        return matchRepository.findActiveById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));
    }

    /** The account that is calling, as a player; an administrator's account has none. */
    private Optional<PlayersEntity> findCurrentPlayer() {
        String email = EmailNormalizer.normalize(CurrentCaller.require().getEmail());
        return playerRepository.findByEmail(email).filter(player -> player.getDeletedAt() == null);
    }

    private PlayersEntity requireCurrentPlayer() {
        return findCurrentPlayer().orElseThrow(() -> new EntityNotFoundException("Player not found"));
    }

    private AttendanceStatus requireValidStatus(String status) {
        if (!AttendanceStatus.isValid(status)) {
            throw new FieldValidationException(STATUS_FIELD, MatchAttendanceError.INVALID_STATUS.name());
        }
        return AttendanceStatus.valueOf(status);
    }

    /**
     * The team the player answers for: of the teams they play for (one per organization), the one that plays this match.
     */
    private UUID requireTeamOfTheMatch(PlayersEntity player, MatchesEntity match) {
        return teamMemberRepository.findAllActiveByPlayerId(player.getId()).stream()
                .map(membership -> membership.getTeam().getId())
                .filter(match::isPlayedBy)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(MatchAttendanceError.NOT_A_PARTICIPANT.name()));
    }

    /** Only a match that is still due to be played has something to confirm: not one that started, ended or was put off. */
    private void requireMatchStillOpen(MatchesEntity match) {
        boolean open = MatchStatus.isAwaitingKickOff(match.getStatus()) && !match.hasStarted(LocalDateTime.now());
        if (!open) {
            throw new IllegalStateException(MatchAttendanceError.MATCH_NOT_OPEN.name());
        }
    }

    private MatchAttendancesEntity newAttendance(UUID matchId, UUID playerId) {
        MatchAttendancesEntity attendance = new MatchAttendancesEntity();
        attendance.setMatchId(matchId);
        attendance.setPlayerId(playerId);
        return attendance;
    }

    private MatchAttendanceResponse toResponse(MatchesEntity match, Optional<PlayersEntity> viewer) {
        List<MatchAttendancesEntity> answers = attendanceRepository.findAllActiveByMatchId(match.getId());
        return new MatchAttendanceResponse(
                match.getId(),
                countOf(answers, AttendanceStatus.CONFIRMED),
                countOf(answers, AttendanceStatus.DECLINED),
                viewer.flatMap(player -> statusOf(answers, player)).orElse(null),
                mayListNames(match, viewer) ? toAttendees(answers) : List.of());
    }

    private int countOf(List<MatchAttendancesEntity> answers, AttendanceStatus status) {
        return (int) answers.stream().filter(answer -> status.name().equals(answer.getStatus())).count();
    }

    private Optional<AttendanceStatus> statusOf(List<MatchAttendancesEntity> answers, PlayersEntity player) {
        return answers.stream()
                .filter(answer -> answer.getPlayerId().equals(player.getId()))
                .map(answer -> AttendanceStatus.valueOf(answer.getStatus()))
                .findFirst();
    }

    /** The names are for the administrators and for the two teams: a public event does not show them to the rest. */
    private boolean mayListNames(MatchesEntity match, Optional<PlayersEntity> viewer) {
        if (CurrentCaller.isAdministrator()) {
            return true;
        }
        return viewer
                .map(player -> teamMemberRepository.findAllActiveByPlayerId(player.getId()).stream()
                        .anyMatch(membership -> match.isPlayedBy(membership.getTeam().getId())))
                .orElse(false);
    }

    private List<AttendeeResponse> toAttendees(List<MatchAttendancesEntity> answers) {
        // Names only, and without the organization filter, like the leaderboards: the players of the two teams are named.
        Map<UUID, String> names = playerRepository.findFullNamesByIds(
                answers.stream().map(MatchAttendancesEntity::getPlayerId).toList());
        return answers.stream()
                .map(answer -> new AttendeeResponse(answer.getPlayerId(), names.get(answer.getPlayerId()), answer.getTeamId(),
                        AttendanceStatus.valueOf(answer.getStatus())))
                .sorted(ATTENDEE_ORDER)
                .toList();
    }
}
