package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.IPlayersService;
import com.platform.backend.modules.players.application.iservices.ITeamMembershipsService;
import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.application.mappers.PlayerResponseAssembler;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.enums.InvitationStatus;
import com.platform.backend.modules.players.domain.enums.InvitationType;
import com.platform.backend.modules.players.domain.exception.TeamInvitationError;
import com.platform.backend.modules.players.domain.exception.PlayerError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.JoinByCodeRequest.JoinByCodeRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerCaptainRequest.UpdatePlayerCaptainRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerStatusRequest.UpdatePlayerStatusRequest;
import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.enums.TeamStatus;
import com.platform.backend.modules.teams.domain.exception.TeamError;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.application.iservices.IEmailVerificationService;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.shared.domain.tenancy.TenantScope;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.domain.utils.JoinCodes;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayersService implements IPlayersService {

    private final IPlayerRepository playerRepository;
    private final IPlayerPhotoRepository playerPhotoRepository;
    private final ITeamRepository teamRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final ITeamMembershipsService teamMembershipsService;
    private final ITeamInvitationRepository teamInvitationRepository;
    private final PlayerMapper playerMapper;
    private final PlayerResponseAssembler playerResponses;
    private final IUsersAuthService usersAuthService;
    private final IEmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @Value("${app.invite.expiration-days:7}")
    private int inviteExpirationDays;

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getAll() {
        List<PlayersEntity> players = playerRepository.findAllActive();
        Map<UUID, List<TeamMembersEntity>> membershipsByPlayer = playerResponses.membershipsByPlayerId(players);
        return players.stream()
                .filter(player -> isListedForCaller(membershipsByPlayer.getOrDefault(player.getId(), List.of())))
                .map(player -> playerResponses.toResponse(player, membershipsByPlayer.getOrDefault(player.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getById(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        return playerResponses.toResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse searchByEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return playerRepository.findByEmail(normalizedEmail)
                .filter(p -> p.getDeletedAt() == null)
                .map(playerResponses::toResponse)
                .or(() -> findPlayerOfAnotherOrganization(normalizedEmail))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    /**
     * Someone who plays only for teams of other organizations is hidden from this administrator by the organization
     * filter, but they are registered all the same: answering "not found" would send the administrator to ask them to
     * register again. Only who they are is revealed (name, email, photo) and that they play for someone else: not which team,
     * nor their phone or captaincy, which belong to the organization of that team.
     */
    private Optional<PlayerResponse> findPlayerOfAnotherOrganization(String normalizedEmail) {
        return playerRepository.findAnyActiveByEmail(normalizedEmail).map(player -> {
            PlayerResponse response = new PlayerResponse();
            response.setId(player.getId());
            response.setFirstName(player.getFirstName());
            response.setLastName(player.getLastName());
            response.setEmail(player.getEmail());
            response.setProfilePhotoUrl(player.getProfilePhotoUrl());
            response.setStatus(player.getStatus());
            response.setInTeamOfAnotherOrganization(true);
            return response;
        });
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
                request.getFirstName(), request.getLastName(), false);
        player.setPhone(request.getPhone());
        player.setProfilePhotoUrl(request.getProfilePhotoUrl());
        PlayersEntity saved = playerRepository.save(player);
        joinIfThereIsATeam(saved, team);
        return playerResponses.toResponse(saved);
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
        TeamsEntity team = hasInvitation ? findInvitingTeam(invitation) : null;

        PlayersEntity player = registerAccountWithPlayer(request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName(), isInvitedEmail(invitation, request.getEmail()));
        player.setPhone(request.getPhone());
        player.setProfilePhotoUrl(request.getProfilePhotoUrl());
        PlayersEntity saved = playerRepository.save(player);
        joinIfThereIsATeam(saved, team);

        if (hasInvitation) {
            invitation.setStatus(InvitationStatus.USED.name());
            teamInvitationRepository.save(invitation);
        }
        return playerResponses.toResponse(saved);
    }

    private void joinIfThereIsATeam(PlayersEntity player, TeamsEntity team) {
        if (team != null) {
            teamMembershipsService.join(player, team);
        }
    }

    /** The refusal reasons are codes, not sentences, so the app can explain each one in the user's language. */
    private TeamInvitationsEntity findUsableInvitation(String token) {
        TeamInvitationsEntity invitation = teamInvitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException(TeamInvitationError.INVITATION_NOT_FOUND.name()));

        if (!InvitationStatus.PENDING.name().equals(invitation.getStatus())) {
            throw new IllegalStateException(TeamInvitationError.NOT_PENDING.name());
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException(TeamInvitationError.EXPIRED.name());
        }
        return invitation;
    }

    private TeamsEntity findInvitingTeam(TeamInvitationsEntity invitation) {
        return teamRepository.findActiveById(invitation.getTeamId())
                .orElseThrow(() -> new IllegalStateException(TeamInvitationError.TEAM_NOT_FOUND.name()));
    }

    /**
     * The link of an invitation only reached the email it was sent to, so whoever registers that same email with it has
     * proved it is theirs. With another email (or without an invitation) nothing is proved.
     */
    private boolean isInvitedEmail(TeamInvitationsEntity invitation, String registeredEmail) {
        return invitation != null
                && Objects.equals(EmailNormalizer.normalize(invitation.getEmail()), EmailNormalizer.normalize(registeredEmail));
    }

    /**
     * A player signs in with an account, so the account is registered the way the app does it, which also creates the
     * player with the same email; the caller then completes that player (phone, photo, team).
     */
    private PlayersEntity registerAccountWithPlayer(String email, String password, String firstName, String lastName,
                                                    boolean emailVerified) {
        RegisterRequest account = new RegisterRequest();
        account.setEmail(email);
        account.setPassword(password);
        account.setFirstName(firstName);
        account.setLastName(lastName);
        if (emailVerified) {
            usersAuthService.registerVerified(account);
        } else {
            usersAuthService.register(account);
        }
        return playerRepository.findByEmail(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new IllegalStateException("The player of the new account was not created"));
    }

    @Override
    @Transactional
    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        playerMapper.updateEntity(player, request);
        PlayersEntity saved = playerRepository.save(player);
        if (request.getTeamId() != null) {
            teamMembershipsService.join(saved, resolveTeam(request.getTeamId()));
        }
        return playerResponses.toResponse(saved);
    }

    @Override
    @Transactional
    public AcceptedInvitationResponse joinByCode(JoinByCodeRequest request) {
        String email = CurrentCaller.require().getEmail();
        emailVerificationService.requireVerified(email);
        PlayersEntity player = playerRepository.findByEmail(EmailNormalizer.normalize(email))
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));
        TeamsEntity team = findTeamOpenToJoinCodes(request.getCode());
        requireNoTeamInTheOrganizationOf(player, team);

        teamMembershipsService.join(player, team);
        PlayerResponse joined = playerResponses.toResponse(player);

        return new AcceptedInvitationResponse(joined, usersAuthService.reissueSession(player.getEmail()).getToken());
    }

    /**
     * A code is not a way to change teams: a player who already plays for a team of that organization has to leave it
     * first. The teams of other organizations do not matter, a player can have one in each.
     */
    private void requireNoTeamInTheOrganizationOf(PlayersEntity player, TeamsEntity team) {
        boolean alreadyInTheOrganization = teamMemberRepository.findAllActiveByPlayerId(player.getId()).stream()
                .anyMatch(membership -> Objects.equals(membership.getOrganizationId(), team.getOrganizationId()));
        if (alreadyInTheOrganization) {
            throw new IllegalStateException(PlayerError.ALREADY_IN_TEAM.name());
        }
    }

    /** A wrong code and a team that is no longer active answer alike: the caller learns nothing about which codes exist. */
    private TeamsEntity findTeamOpenToJoinCodes(String typedCode) {
        return teamRepository.findActiveByJoinCode(JoinCodes.normalize(typedCode))
                .filter(team -> TeamStatus.ACTIVE.name().equals(team.getStatus()))
                .orElseThrow(() -> new EntityNotFoundException(TeamError.JOIN_CODE_NOT_FOUND.name()));
    }

    @Override
    @Transactional
    public void invitePlayer(InvitePlayerRequest request) {
        // Whatever organization they play for: a registered person is never asked to register again.
        PlayersEntity player = playerRepository.findAnyActiveByEmail(EmailNormalizer.normalize(request.getEmail()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        TeamsEntity team = teamRepository.findActiveById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));

        inviteRegisteredPlayer(player, team);
    }

    /** They join only if they accept it in the app; a team they have in the same organization is then replaced. */
    private void inviteRegisteredPlayer(PlayersEntity player, TeamsEntity team) {
        // The invited person finds it by the email of their account, which is the player's one.
        TeamInvitationsEntity invitation = new TeamInvitationsEntity();
        invitation.setEmail(player.getEmail());
        invitation.setTeamId(team.getId());
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

        String email = EmailNormalizer.normalize(request.getEmail());

        // Registering twice is impossible (the email is taken): whoever is already registered is asked to join instead.
        Optional<PlayersEntity> alreadyRegistered = playerRepository.findAnyActiveByEmail(email);
        if (alreadyRegistered.isPresent()) {
            inviteRegisteredPlayer(alreadyRegistered.get(), team);
            return;
        }

        String token = UUID.randomUUID().toString();
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
        return teamMemberRepository.findAllActiveByTeamId(teamId).stream()
                .map(membership -> playerResponses.toResponse(membership.getPlayer(), List.of(membership)))
                .toList();
    }

    @Override
    @Transactional
    public PlayerResponse removeFromTeam(UUID id, UUID teamId) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        TeamMembersEntity membership = findMembershipToChange(player, teamId)
                .orElseThrow(() -> new IllegalStateException("Player does not belong to any team"));
        teamMembershipsService.leave(membership);
        return playerResponses.toResponse(player);
    }

    @Override
    @Transactional
    public PlayerResponse updateStatus(UUID id, UpdatePlayerStatusRequest request) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        player.setStatus(request.getStatus());
        return playerResponses.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public PlayerResponse updateCaptain(UUID id, UpdatePlayerCaptainRequest request) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        TeamMembersEntity membership = findMembershipToChange(player, request.getTeamId())
                .orElseThrow(() -> new IllegalStateException(PlayerError.CAPTAIN_REQUIRES_TEAM.name()));

        if (Boolean.TRUE.equals(request.getIsCaptain())) {
            stepDownOtherCaptainsOf(membership);
        }
        membership.setIsCaptain(request.getIsCaptain());
        teamMemberRepository.save(membership);
        return playerResponses.toResponse(player);
    }

    /**
     * The team a change is about: the one asked for, or the only one the caller sees the player in. An administrator of an
     * organization always sees one at most; whoever sees several (the platform owner) has to say which. Empty when the player
     * plays for none the caller sees.
     */
    private Optional<TeamMembersEntity> findMembershipToChange(PlayersEntity player, UUID teamId) {
        List<TeamMembersEntity> memberships = teamMemberRepository.findAllActiveByPlayerId(player.getId());
        if (teamId != null) {
            return memberships.stream().filter(membership -> membership.getTeam().getId().equals(teamId)).findFirst();
        }
        if (memberships.size() > 1) {
            throw new IllegalStateException(PlayerError.TEAM_REQUIRED.name());
        }
        return memberships.stream().findFirst();
    }

    /** The previous captain is saved first: the unique index allows one captain per team at every moment. */
    private void stepDownOtherCaptainsOf(TeamMembersEntity newCaptain) {
        teamMemberRepository.findAllActiveByTeamId(newCaptain.getTeam().getId()).stream()
                .filter(teammate -> teammate.isCaptainOfTheTeam() && !teammate.getId().equals(newCaptain.getId()))
                .forEach(previousCaptain -> {
                    previousCaptain.setIsCaptain(false);
                    teamMemberRepository.saveAndFlush(previousCaptain);
                });
    }

    /** A deleted player keeps no photo: the bytes are not data worth keeping for someone who is gone. */
    @Override
    @Transactional
    public void delete(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        requireNotPlayingForAnotherOrganization(player);
        teamMembershipsService.leaveEvery(player);
        player.setDeletedAt(LocalDateTime.now());
        playerRepository.save(player);
        playerPhotoRepository.deleteByPlayerId(id);
    }

    @Override
    @Transactional
    public void hardDelete(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        requireNotPlayingForAnotherOrganization(player);
        playerRepository.hardDelete(player);
    }

    /**
     * A person is shared by every organization they play for, so one organization cannot erase them: it can only take
     * them out of its own team. The platform owner (who sees everything) can.
     */
    private void requireNotPlayingForAnotherOrganization(PlayersEntity player) {
        TenantScope scope = CurrentTenant.scope();
        if (scope.restricted()
                && teamMemberRepository.existsActiveOutsideOrganization(player.getId(), scope.organizationIdForFilter())) {
            throw new IllegalStateException(PlayerError.PLAYS_FOR_ANOTHER_ORGANIZATION.name());
        }
    }

    /**
     * A player with no team is visible to everyone so that it can be found by email and added to a team, but it is
     * not listed to an organization's administrator: that list is for their own teams' players.
     */
    private boolean isListedForCaller(List<TeamMembersEntity> visibleMemberships) {
        return CurrentTenant.scope().isUnrestricted() || !visibleMemberships.isEmpty();
    }

    private TeamsEntity resolveTeam(UUID teamId) {
        if (teamId == null) return null;
        return teamRepository.findActiveById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
    }
}
