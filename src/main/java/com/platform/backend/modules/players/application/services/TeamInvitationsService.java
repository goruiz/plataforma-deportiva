package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.ITeamInvitationsService;
import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.application.mappers.TeamInvitationMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.enums.InvitationStatus;
import com.platform.backend.modules.players.domain.enums.InvitationType;
import com.platform.backend.modules.players.domain.exception.TeamInvitationError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.players.presentation.responses.TeamInvitationResponse.TeamInvitationResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamInvitationsService implements ITeamInvitationsService {

    private final ITeamInvitationRepository invitationRepository;
    private final IPlayerRepository playerRepository;
    private final ITeamRepository teamRepository;
    private final TeamInvitationMapper invitationMapper;
    private final PlayerMapper playerMapper;
    private final IPlayerAccountOrganizationService playerAccountOrganizationService;
    private final IUsersAuthService usersAuthService;

    /** Only those for someone who already has an account; the others are answered by registering with their link. */
    @Override
    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> getMyPendingInvitations() {
        List<TeamInvitationsEntity> pending = invitationRepository
                .findAllActiveByEmailAndStatus(CurrentCaller.require().getEmail(), InvitationStatus.PENDING.name())
                .stream()
                .filter(invitation -> InvitationType.EXISTING_USER.name().equals(invitation.getType()))
                .filter(invitation -> !hasExpired(invitation))
                .toList();
        return withTeamNames(pending);
    }

    @Override
    @Transactional
    public AcceptedInvitationResponse accept(UUID invitationId) {
        TeamInvitationsEntity invitation = findAnswerableInvitation(invitationId);
        TeamsEntity team = teamRepository.findActiveById(invitation.getTeamId())
                .orElseThrow(() -> new IllegalStateException(TeamInvitationError.TEAM_NOT_FOUND.name()));
        PlayersEntity player = playerRepository.findByEmail(CurrentCaller.require().getEmail())
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalStateException(TeamInvitationError.PLAYER_NOT_FOUND.name()));

        // A player has a single team, so accepting also leaves the previous one.
        player.setTeam(team);
        invitation.setStatus(InvitationStatus.USED.name());
        invitationRepository.save(invitation);
        PlayerResponse joined = playerMapper.toResponse(playerRepository.save(player));

        // What the player sees follows their team, and their open token still names the old organization.
        playerAccountOrganizationService.followTeam(player.getEmail(), team.getOrganizationId());
        return new AcceptedInvitationResponse(joined, usersAuthService.reissueSession(player.getEmail()).getToken());
    }

    @Override
    @Transactional
    public void decline(UUID invitationId) {
        TeamInvitationsEntity invitation = findAnswerableInvitation(invitationId);
        invitation.setStatus(InvitationStatus.DECLINED.name());
        invitationRepository.save(invitation);
    }

    /**
     * An invitation can only be answered by the person it was sent to: anyone else gets a 404, as if it did not
     * exist, and one already answered or expired is a 400 with the reason as its code.
     */
    private TeamInvitationsEntity findAnswerableInvitation(UUID invitationId) {
        String callerEmail = CurrentCaller.require().getEmail();
        TeamInvitationsEntity invitation = invitationRepository.findById(invitationId)
                .filter(found -> found.getDeletedAt() == null)
                .filter(found -> found.getEmail().equalsIgnoreCase(callerEmail))
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with id: " + invitationId));
        if (!InvitationStatus.PENDING.name().equals(invitation.getStatus())) {
            throw new IllegalStateException(TeamInvitationError.NOT_PENDING.name());
        }
        if (hasExpired(invitation)) {
            throw new IllegalStateException(TeamInvitationError.EXPIRED.name());
        }
        return invitation;
    }

    private boolean hasExpired(TeamInvitationsEntity invitation) {
        return invitation.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /** An invitation only holds the id of its team, so the names are read once for the whole list. */
    private List<TeamInvitationResponse> withTeamNames(List<TeamInvitationsEntity> invitations) {
        Map<UUID, String> teamNames = teamRepository
                .findAllActiveByIds(invitations.stream().map(TeamInvitationsEntity::getTeamId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(TeamsEntity::getId, TeamsEntity::getName));
        return invitations.stream().map(invitation -> {
            TeamInvitationResponse response = invitationMapper.toResponse(invitation);
            response.setTeamName(teamNames.get(invitation.getTeamId()));
            return response;
        }).toList();
    }
}
