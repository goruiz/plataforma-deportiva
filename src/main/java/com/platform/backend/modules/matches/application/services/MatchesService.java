package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest.CreateMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchesService implements IMatchesService {

    private final IMatchRepository matchRepository;
    private final MatchMapper matchMapper;

    @Override
    public List<MatchResponse> getAll() {
        return matchRepository.findAllActive()
                .stream()
                .map(matchMapper::toResponse)
                .toList();
    }

    @Override
    public MatchResponse getById(UUID id) {
        MatchesEntity match = matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        return matchMapper.toResponse(match);
    }

    @Override
    public MatchResponse create(CreateMatchRequest request) {
        MatchesEntity match = matchMapper.toEntity(request);
        return matchMapper.toResponse(matchRepository.save(match));
    }

    @Override
    public MatchResponse update(UUID id, UpdateMatchRequest request) {
        MatchesEntity match = matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        matchMapper.updateEntity(match, request);
        return matchMapper.toResponse(matchRepository.save(match));
    }

    @Override
    public void delete(UUID id) {
        MatchesEntity match = matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        match.setDeletedAt(LocalDateTime.now());
        matchRepository.delete(match);
    }

    @Override
    public void hardDelete(UUID id) {
        MatchesEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        matchRepository.hardDelete(match);
    }
}
