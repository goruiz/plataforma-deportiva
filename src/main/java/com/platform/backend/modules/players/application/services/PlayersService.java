package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.IPlayersService;
import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.email.EmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayersService implements IPlayersService {

    private final IPlayerRepository playerRepository;
    private final ITeamRepository teamRepository;
    private final ITeamInvitationRepository teamInvitationRepository;
    private final PlayerMapper playerMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.invite.expiration-days:7}")
    private int inviteExpirationDays;

    @Override
    public List<PlayerResponse> getAll() {
        return playerRepository.findAllActive()
                .stream()
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    public PlayerResponse getById(UUID id) {
        PlayersEntity player = playerRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + id));
        return playerMapper.toResponse(player);
    }

    @Override
    public PlayerResponse searchByEmail(String email) {
        PlayersEntity player = playerRepository.findByEmail(email)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return playerMapper.toResponse(player);
    }

    @Override
    public PlayerResponse create(CreatePlayerRequest request) {
        if (playerRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email already registered");
        }
        PlayersEntity player = playerMapper.toEntity(request);
        player.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        player.setTeam(resolveTeam(request.getTeamId()));
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public PlayerResponse registerWithInvite(PlayerRegisterRequest request) {
        if (playerRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email already registered");
        }
        PlayersEntity player = playerMapper.toEntity(request);
        player.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if (request.getInviteToken() != null && !request.getInviteToken().isBlank()) {
            TeamInvitationsEntity invitation = teamInvitationRepository.findByToken(request.getInviteToken())
                    .orElseThrow(() -> new EntityNotFoundException("Invalid invitation token"));

            if (!"PENDING".equals(invitation.getStatus())) {
                throw new IllegalStateException("Invitation token has already been used or expired");
            }
            if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Invitation token has expired");
            }

            player.setTeam(resolveTeam(invitation.getTeamId()));
            invitation.setStatus("USED");
            teamInvitationRepository.save(invitation);
        }

        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
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
        PlayersEntity player = playerRepository.findByEmail(request.getEmail())
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        TeamsEntity team = teamRepository.findActiveById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));

        TeamInvitationsEntity invitation = new TeamInvitationsEntity();
        invitation.setEmail(request.getEmail());
        invitation.setTeamId(request.getTeamId());
        invitation.setType("EXISTING_USER");
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(inviteExpirationDays));
        teamInvitationRepository.save(invitation);

        String playerName = player.getFirstName() + " " + player.getLastName();
        emailService.sendTeamInvitation(request.getEmail(), playerName, team.getName());
    }

    @Override
    @Transactional
    public void inviteRegister(InviteRegisterRequest request) {
        TeamsEntity team = teamRepository.findActiveById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));

        String token = UUID.randomUUID().toString();

        TeamInvitationsEntity invitation = new TeamInvitationsEntity();
        invitation.setEmail(request.getEmail());
        invitation.setTeamId(request.getTeamId());
        invitation.setToken(token);
        invitation.setType("NEW_USER");
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(inviteExpirationDays));
        teamInvitationRepository.save(invitation);

        emailService.sendRegistrationInvitation(request.getEmail(), team.getName(), token);
    }

    @Override
    public List<PlayerResponse> getByTeamId(UUID teamId) {
        return playerRepository.findAllActiveByTeamId(teamId)
                .stream()
                .map(playerMapper::toResponse)
                .toList();
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

    private TeamsEntity resolveTeam(UUID teamId) {
        if (teamId == null) return null;
        return teamRepository.findActiveById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
    }
}
