package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.teams.application.iservices.ITeamsService;
import com.platform.backend.modules.teams.application.mappers.TeamMapper;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest.CreateTeamRequest;
import com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest.UpdateTeamRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamResponse.TeamResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamsService implements ITeamsService {

    private final ITeamRepository teamRepository;
    private final TeamMapper teamMapper;

    @Override
    public List<TeamResponse> getAll() {
        return teamRepository.findAllActive()
                .stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    @Override
    public TeamResponse getById(UUID id) {
        TeamsEntity team = teamRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
        return teamMapper.toResponse(team);
    }

    @Override
    public TeamResponse create(CreateTeamRequest request) {
        if (teamRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Team name already exists");
        }
        TeamsEntity team = teamMapper.toEntity(request);
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public TeamResponse update(UUID id, UpdateTeamRequest request) {
        TeamsEntity team = teamRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
        teamMapper.updateEntity(team, request);
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public void delete(UUID id) {
        TeamsEntity team = teamRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
        team.setDeletedAt(LocalDateTime.now());
        teamRepository.delete(team);
    }

    @Override
    public void hardDelete(UUID id) {
        TeamsEntity team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
        teamRepository.hardDelete(team);
    }
}

