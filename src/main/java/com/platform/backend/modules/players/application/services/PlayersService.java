package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.IPlayersService;
import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.enums.InvitationStatus;
import com.platform.backend.modules.players.domain.enums.InvitationType;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerStatusRequest.UpdatePlayerStatusRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayersService implements IPlayersService {

    private final IPlayerRepository playerRepository;
    private final ITeamRepository teamRepository;
    private final ITeamInvitationRepository teamInvitationRepository;
    private final PlayerMapper playerMapper;
    private final IUsersAuthService usersAuthService;
    private final EmailService emailService;

    @Value("${app.invite.expiration-days:7}")
    private int inviteExpirationDays;

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getAll() {
        return playerRepository.findAllActive()
                .stream()
                .filter(this::isListedForCaller)
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getById(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse searchByEmail(String email) {
        PlayersEntity player = playerRepository.findByEmail(EmailNormalizer.normalize(email))
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getCurrent() {
        return searchByEmail(CurrentCaller.require().getEmail());
    }

    @Override
    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        TeamsEntity team = resolveTeam(request.getTeamId());
        PlayersEntity player = registerAccountWithPlayer(request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName());
        player.setPhone(request.getPhone());
        player.setProfilePhotoUrl(request.getProfilePhotoUrl());
        player.setTeam(team);
        return playerMapper.toResponse(playerRepository.save(player));
    }

    /**
     * Registering with an invitation creates the account too, so the player can sign in with the password they just
     * chose: the invitation only decides the team they join.
     */
    @Override
    @Transactional
    public PlayerResponse registerWithInvite(PlayerRegisterRequest request) {
        boolean hasInvitation = request.getInviteToken() != null && !request.getInviteToken().isBlank();
        TeamInvitationsEntity invitation = hasInvitation ? findUsableInvitation(request.getInviteToken()) : null;
        TeamsEntity team = hasInvitation ? resolveTeam(invitation.getTeamId()) : null;

        PlayersEntity player = registerAccountWithPlayer(request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName());
        player.setPhone(request.getPhone());
        player.setProfilePhotoUrl(request.getProfilePhotoUrl());
        player.setTeam(team);

        if (hasInvitation) {
            invitation.setStatus(InvitationStatus.USED.name());
            teamInvitationRepository.save(invitation);
        }
        return playerMapper.toResponse(playerRepository.save(player));
    }

    private TeamInvitationsEntity findUsableInvitation(String token) {
        TeamInvitationsEntity invitation = teamInvitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invitation token"));

        if (!InvitationStatus.PENDING.name().equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation token has already been used or expired");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Invitation token has expired");
        }
        return invitation;
    }

    /**
     * A player signs in with an account, so the account is registered the way the app does it, which also creates the
     * player with the same email; the caller then completes that player (phone, photo, team).
     */
    private PlayersEntity registerAccountWithPlayer(String email, String password, String firstName, String lastName) {
        RegisterRequest account = new RegisterRequest();
        account.setEmail(email);
        account.setPassword(password);
        account.setFirstName(firstName);
        account.setLastName(lastName);
        usersAuthService.register(account);
        return playerRepository.findByEmail(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new IllegalStateException("The player of the new account was not created"));
    }

    @Override
    @Transactional
    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        playerMapper.updateEntity(player, request);
        if (request.getTeamId() != null) {
            player.setTeam(resolveTeam(request.getTeamId()));
        }
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public void invitePlayer(InvitePlayerRequest request) {
        PlayersEntity player = playerRepository.findByEmail(EmailNormalizer.normalize(request.getEmail()))
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        TeamsEntity team = teamRepository.findActiveById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));

        // The invited person finds it by the email of their account, which is the player's one.
        TeamInvitationsEntity invitation = new TeamInvitationsEntity();
        invitation.setEmail(player.getEmail());
        invitation.setTeamId(request.getTeamId());
        invitation.setType(InvitationType.EXISTING_USER.name());
        invitation.setStatus(InvitationStatus.PENDING.name());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(inviteExpirationDays));
        teamInvitationRepository.save(invitation);

        String playerName = player.getFirstName() + " " + player.getLastName();
        emailService.sendTeamInvitation(player.getEmail(), playerName, team.getName());
    }

    @Override
    @Transactional
    public void inviteRegister(InviteRegisterRequest request) {
        TeamsEntity team = teamRepository.findActiveById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));

        String token = UUID.randomUUID().toString();

        String email = EmailNormalizer.normalize(request.getEmail());
        TeamInvitationsEntity invitation = new TeamInvitationsEntity();
        invitation.setEmail(email);
        invitation.setTeamId(request.getTeamId());
        invitation.setToken(token);
        invitation.setType(InvitationType.NEW_USER.name());
        invitation.setStatus(InvitationStatus.PENDING.name());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(inviteExpirationDays));
        teamInvitationRepository.save(invitation);

        emailService.sendRegistrationInvitation(email, team.getName(), token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getByTeamId(UUID teamId) {
        return playerRepository.findAllActiveByTeamId(teamId)
                .stream()
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PlayerResponse removeFromTeam(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        if (player.getTeam() == null) {
            throw new IllegalStateException("Player does not belong to any team");
        }
        player.setTeam(null);
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public PlayerResponse updateStatus(UUID id, UpdatePlayerStatusRequest request) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        player.setStatus(request.getStatus());
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    public void delete(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        player.setDeletedAt(LocalDateTime.now());
        playerRepository.save(player);
    }

    @Override
    public void hardDelete(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        playerRepository.hardDelete(player);
    }

    /**
     * A player with no team is visible to everyone so that it can be found by email and added to a team, but it is
     * not listed to an organization's administrator: that list is for their own teams' players.
     */
    private boolean isListedForCaller(PlayersEntity player) {
        return CurrentTenant.scope().isUnrestricted() || player.getTeam() != null;
    }

    private TeamsEntity resolveTeam(UUID teamId) {
        if (teamId == null) return null;
        return teamRepository.findActiveById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
    }
}

