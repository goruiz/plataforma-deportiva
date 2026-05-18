package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.IPlayersService;
import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayersService implements IPlayersService {

    private final IPlayerRepository playerRepository;
    private final ITeamRepository teamRepository;
    private final PlayerMapper playerMapper;
    private final PasswordEncoder passwordEncoder;

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
