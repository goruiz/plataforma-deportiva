package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.teams.application.iservices.ITeamsEventsService;
import com.platform.backend.modules.teams.application.mappers.TeamsEventsMapper;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamsEventsRequest.CreateTeamsEventsRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamsEventsResponse.TeamsEventsResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamsEventsService implements ITeamsEventsService {

    private final ITeamsEventsRepository teamsEventsRepository;
    private final ITeamRepository teamRepository;
    private final IEventRepository eventRepository;
    private final TeamsEventsMapper teamsEventsMapper;

    @Override
    public List<TeamsEventsResponse> getAll() {
        return teamsEventsRepository.findAllActive()
                .stream()
                .map(teamsEventsMapper::toResponse)
                .toList();
    }

    @Override
    public List<TeamsEventsResponse> getByEventId(UUID eventId) {
        return teamsEventsRepository.findAllActiveByEventId(eventId)
                .stream()
                .map(teamsEventsMapper::toResponse)
                .toList();
    }

    @Override
    public TeamsEventsResponse getById(UUID id) {
        TeamsEventsEntity teamsEvents = teamsEventsRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team-event assignment not found with id: " + id));
        return teamsEventsMapper.toResponse(teamsEvents);
    }

    @Override
    public TeamsEventsResponse create(CreateTeamsEventsRequest request) {
        TeamsEntity team = teamRepository.findActiveById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));
        EventsEntity event = eventRepository.findActiveById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + request.getEventId()));

        TeamsEventsEntity teamsEvents = new TeamsEventsEntity();
        teamsEvents.setTeam(team);
        teamsEvents.setEvent(event);

        return teamsEventsMapper.toResponse(teamsEventsRepository.save(teamsEvents));
    }

    @Override
    public void delete(UUID id) {
        TeamsEventsEntity teamsEvents = teamsEventsRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team-event assignment not found with id: " + id));
        teamsEvents.setDeletedAt(LocalDateTime.now());
        teamsEventsRepository.delete(teamsEvents);
    }

    @Override
    public void hardDelete(UUID id) {
        TeamsEventsEntity teamsEvents = teamsEventsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team-event assignment not found with id: " + id));
        teamsEventsRepository.hardDelete(teamsEvents);
    }
}
