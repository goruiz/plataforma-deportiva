package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IMatchDisputesService;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchDisputeResponseAssembler;
import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.DisputeStatus;
import com.platform.backend.modules.matches.domain.exception.DisputeError;
import com.platform.backend.modules.matches.domain.filters.DisputeFilter;
import com.platform.backend.modules.matches.domain.irepositories.IMatchDisputeRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.OpenDisputeRequest.OpenDisputeRequest;
import com.platform.backend.modules.matches.presentation.requests.ResolveDisputeRequest.ResolveDisputeRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.DisputeCountsResponse.DisputeCountsResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchDisputeResponse.MatchDisputeResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.domain.security.CallerPrincipal;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CreatorAccess;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MatchDisputesService implements IMatchDisputesService {

    private static final String TEAM_FIELD = "teamId";
    private static final String REASON_FIELD = "reason";
    private static final String NOTE_FIELD = "note";
    private static final String HOME_SCORE_FIELD = "homeScore";
    private static final String PROPOSED_HOME_SCORE_FIELD = "proposedHomeScore";

    private final IMatchDisputeRepository disputeRepository;
    private final IMatchRepository matchRepository;
    private final IEventRepository eventRepository;
    private final IMatchesService matchesService;
    private final MatchSettingsReader settingsReader;
    private final IUserRepository userRepository;
    private final IPlayerRepository playerRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final MatchDisputeResponseAssembler assembler;

    // ------------------------------------------------------------------ opening

    @Override
    @Transactional
    public MatchDisputeResponse open(UUID matchId, OpenDisputeRequest request) {
        MatchesEntity match = findMatchOrThrow(matchId);
        UUID teamId = request.getTeamId();
        requireTeamPlaysTheMatch(match, teamId);
        requireMayDisputeFor(teamId);
        requireResultRecorded(match);
        requireWindowOpen(match);
        requireNoOpenDispute(matchId, teamId);

        MatchDisputesEntity dispute = newDispute(matchId, request);
        disputeRepository.save(dispute);
        stopValidatingTheResult(match);
        return assembler.toResponses(List.of(dispute)).get(0);
    }

    private void requireTeamPlaysTheMatch(MatchesEntity match, UUID teamId) {
        if (!match.isPlayedBy(teamId)) {
            throw new FieldValidationException(TEAM_FIELD, DisputeError.TEAM_NOT_IN_MATCH.name());
        }
    }

    /**
     * An administrator disputes for anyone; everyone else has to be a "dirigente de equipo" (the account capability
     * {@code OPEN_MATCH_DISPUTES}) and belong to the team they speak for. Being the captain no longer is enough.
     */
    private void requireMayDisputeFor(UUID teamId) {
        if (CurrentCaller.isAdministrator() || (callerCanOpenDisputes() && belongsToTeam(teamId))) {
            return;
        }
        throw new AccessDeniedException(DisputeError.NOT_ALLOWED_FOR_TEAM.name());
    }

    private boolean callerCanOpenDisputes() {
        return accountCapabilities.has(CurrentCaller.require().getId(), AccountCapability.OPEN_MATCH_DISPUTES);
    }

    private boolean belongsToTeam(UUID teamId) {
        return playerOfCaller()
                .flatMap(player -> teamMemberRepository.findActiveByPlayerIdAndTeamId(player.getId(), teamId))
                .isPresent();
    }

    private void requireResultRecorded(MatchesEntity match) {
        if (!match.hasResult()) {
            throw new IllegalStateException(DisputeError.RESULT_NOT_RECORDED.name());
        }
    }

    /** The window counts from when the result was recorded; an administrator can dispute on a team's behalf at any time. */
    private void requireWindowOpen(MatchesEntity match) {
        if (CurrentCaller.isAdministrator()) {
            return;
        }
        LocalDateTime recordedAt = match.getResultRecordedAt() != null ? match.getResultRecordedAt() : match.getUpdatedAt();
        int windowHours = settingsReader.of(match).disputeWindowHours();
        if (recordedAt != null && LocalDateTime.now().isAfter(recordedAt.plusHours(windowHours))) {
            throw new IllegalStateException(DisputeError.WINDOW_CLOSED.name());
        }
    }

    private void requireNoOpenDispute(UUID matchId, UUID teamId) {
        if (disputeRepository.existsOpenByMatchIdAndTeamId(matchId, teamId)) {
            throw new IllegalStateException(DisputeError.ALREADY_OPEN.name());
        }
    }

    private MatchDisputesEntity newDispute(UUID matchId, OpenDisputeRequest request) {
        if (!StringUtils.hasText(request.getReason())) {
            throw new FieldValidationException(REASON_FIELD, DisputeError.REASON_REQUIRED.name());
        }
        requireScorePair(request.getProposedHomeScore(), request.getProposedAwayScore(), PROPOSED_HOME_SCORE_FIELD);
        MatchDisputesEntity dispute = new MatchDisputesEntity();
        dispute.setMatchId(matchId);
        dispute.setTeamId(request.getTeamId());
        dispute.setReason(request.getReason().trim());
        dispute.setProposedHomeScore(request.getProposedHomeScore());
        dispute.setProposedAwayScore(request.getProposedAwayScore());
        dispute.setOpenedByName(nameOfCaller());
        return dispute;
    }

    /** A result under dispute is not official: it has to be validated again once the dispute is settled. */
    private void stopValidatingTheResult(MatchesEntity match) {
        if (match.getResultValidatedAt() != null) {
            match.setResultValidatedAt(null);
            match.setResultValidatedBy(null);
            matchRepository.save(match);
        }
    }

    // ------------------------------------------------------------------ reading

    @Override
    public PageResult<MatchDisputeResponse> search(DisputeFilter filter, PageQuery pageQuery) {
        PageResult<MatchDisputesEntity> page = disputeRepository.findPage(filter, pageQuery);
        List<MatchDisputeResponse> responses = assembler.toResponses(page.content());
        return new PageResult<>(responses, page.totalElements(), pageQuery);
    }

    @Override
    public DisputeCountsResponse countByStatus(DisputeFilter filter) {
        DisputeFilter ofAnyStatus = filter.withStatus(null);
        DisputeCountsResponse counts = new DisputeCountsResponse();
        counts.setAll(disputeRepository.count(ofAnyStatus));
        counts.setOpen(disputeRepository.count(ofAnyStatus.withStatus(DisputeStatus.OPEN)));
        counts.setAccepted(disputeRepository.count(ofAnyStatus.withStatus(DisputeStatus.ACCEPTED)));
        counts.setRejected(disputeRepository.count(ofAnyStatus.withStatus(DisputeStatus.REJECTED)));
        return counts;
    }

    @Override
    public List<MatchDisputeResponse> getByMatch(UUID matchId) {
        findMatchOrThrow(matchId);
        List<MatchDisputesEntity> disputes = disputeRepository.findAllActiveByMatchId(matchId);
        if (!CurrentCaller.isAdministrator()) {
            // Only a dirigente sees the disputes of their own teams; for anyone else there are none to show.
            Set<UUID> ownTeams = callerCanOpenDisputes() ? teamIdsOfCaller() : Set.of();
            disputes = disputes.stream().filter(dispute -> ownTeams.contains(dispute.getTeamId())).toList();
        }
        return assembler.toResponses(disputes);
    }

    /** The disputes of the teams of a dirigente (the app); an account without that capability has none to see. */
    @Override
    public List<MatchDisputeResponse> getMine() {
        if (!callerCanOpenDisputes()) {
            return List.of();
        }
        return assembler.toResponses(disputeRepository.findAllActiveByTeamIds(teamIdsOfCaller()));
    }

    // ------------------------------------------------------------------ resolving

    @Override
    @Transactional
    public MatchDisputeResponse resolve(UUID id, ResolveDisputeRequest request) {
        MatchDisputesEntity dispute = disputeRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispute not found with id: " + id));
        MatchesEntity match = findMatchOrThrow(dispute.getMatchId());
        CreatorAccess.requireAdminOrCreator(match.getEventId(), eventRepository::findActiveById);
        if (!dispute.isOpen()) {
            throw new IllegalStateException(DisputeError.NOT_OPEN.name());
        }

        if (Boolean.TRUE.equals(request.getAccepted())) {
            correctTheResult(match, request);
        } else if (!StringUtils.hasText(request.getNote())) {
            throw new FieldValidationException(NOTE_FIELD, DisputeError.NOTE_REQUIRED.name());
        }
        markResolved(dispute, request);
        disputeRepository.save(dispute);
        return assembler.toResponses(List.of(dispute)).get(0);
    }

    /**
     * Upholding a dispute may come with the right result. It goes through the match's own update, so it meets the same
     * rules as recording one (the match is over, draws and penalties follow the settings) and, as any change of result,
     * leaves it unvalidated. If it is refused the dispute stays open: the whole decision is one transaction.
     */
    private void correctTheResult(MatchesEntity match, ResolveDisputeRequest request) {
        if (request.getHomeScore() == null && request.getAwayScore() == null) {
            return;
        }
        requireScorePair(request.getHomeScore(), request.getAwayScore(), HOME_SCORE_FIELD);
        UpdateMatchRequest correction = new UpdateMatchRequest();
        correction.setHomeScore(request.getHomeScore());
        correction.setAwayScore(request.getAwayScore());
        correction.setHomePenaltyScore(request.getHomePenaltyScore());
        correction.setAwayPenaltyScore(request.getAwayPenaltyScore());
        matchesService.update(match.getId(), correction);
    }

    private void markResolved(MatchDisputesEntity dispute, ResolveDisputeRequest request) {
        CallerPrincipal caller = CurrentCaller.require();
        dispute.setStatus(Boolean.TRUE.equals(request.getAccepted()) ? DisputeStatus.ACCEPTED : DisputeStatus.REJECTED);
        dispute.setResolutionNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
        dispute.setResolvedBy(caller.getId());
        dispute.setResolvedByName(nameOfCaller());
        dispute.setResolvedAt(LocalDateTime.now());
    }

    // ------------------------------------------------------------------ helpers

    private MatchesEntity findMatchOrThrow(UUID matchId) {
        return matchRepository.findActiveById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));
    }

    /** Both goals or none, and never negative. {@code firstField} is the one a rejection points at. */
    private static void requireScorePair(Integer home, Integer away, String firstField) {
        if ((home == null) != (away == null)) {
            throw new FieldValidationException(firstField, DisputeError.SCORE_INCOMPLETE.name());
        }
        if (home != null && (home < 0 || away < 0)) {
            throw new FieldValidationException(firstField, DisputeError.INVALID_SCORE.name());
        }
    }

    private Optional<PlayersEntity> playerOfCaller() {
        return playerRepository.findByEmail(CurrentCaller.require().getEmail())
                .filter(player -> player.getDeletedAt() == null);
    }

    private Set<UUID> teamIdsOfCaller() {
        return playerOfCaller()
                .map(player -> teamMemberRepository.findAllActiveByPlayerId(player.getId()).stream()
                        .map(membership -> membership.getTeam().getId())
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    /** How the caller is called on screen: their name, or their email when the account has none. */
    private String nameOfCaller() {
        CallerPrincipal caller = CurrentCaller.require();
        return userRepository.findActiveById(caller.getId())
                .map(MatchDisputesService::displayName)
                .orElse(caller.getEmail());
    }

    privat