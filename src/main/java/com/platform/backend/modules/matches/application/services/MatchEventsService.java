package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchEventsService;
import com.platform.backend.modules.matches.application.mappers.MatchEventMapper;
import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchEventRequest.CreateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest.UpdateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchEventResponse.MatchEventResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchEventsService implements IMatchEventsService {

    private final IMatchEventRepository matchEventRepository;
    private final MatchEventMapper matchEventMapper;

    @Override
    public List<MatchEventResponse> getAll() {
        return matchEventRepository.findAllActive()
                .stream()
                .map(matchEventMapper::toResponse)
                .toList();
    }

    @Override
    public MatchEventResponse getById(UUID id) {
        MatchEventsEntity matchEvent = matchEventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match event not found with id: " + id));
        return matchEventMapper.toResponse(matchEvent);
    }

    @Override
    public MatchEventResponse create(CreateMatchEventRequest request) {
        MatchEventsEntity matchEvent = matchEventMapper.toEntity(request);
        return matchEventMapper.toResponse(matchEventRepository.save(matchEvent));
    }

    @Override
    public MatchEventResponse update(UUID id, UpdateMatchEventRequest request) {
        MatchEventsEntity matchEvent = matchEventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match event not found with id: " + id));
        matchEventMapper.updateEntity(matchEvent, request);
        return matchEventMapper.toResponse(matchEventRepository.save(matchEvent));
    }

    @Override
    public void delete(UUID id) {
        MatchEventsEntity matchEvent = matchEventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match event not found with id: " + id));
        matchEvent.setDeletedAt(LocalDateTime.now());
        matchEventRepository.delete(matchEvent);
    }

    @Override
    public void hardDelete(UUID id) {
        MatchEventsEntity matchEvent = matchEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match event not found with id: " + id));
        matchEventRepository.hardDelete(matchEvent);
    }
}
