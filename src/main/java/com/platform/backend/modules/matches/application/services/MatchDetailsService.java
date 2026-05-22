package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchDetailsService;
import com.platform.backend.modules.matches.application.mappers.MatchDetailMapper;
import com.platform.backend.modules.matches.domain.entities.ActionsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchDetailsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IActionRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchDetailRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchDetailRequest.CreateMatchDetailRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchDetailRequest.UpdateMatchDetailRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchDetailResponse.MatchDetailResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchDetailsService implements IMatchDetailsService {

    private final IMatchDetailRepository matchDetailRepository;
    private final IMatchRepository matchRepository;
    private final IActionRepository actionRepository;
    private final MatchDetailMapper matchDetailMapper;

    @Override
    public List<MatchDetailResponse> getAll() {
        return matchDetailRepository.findAllActive()
                .stream()
                .map(matchDetailMapper::toResponse)
                .toList();
    }

    @Override
    public MatchDetailResponse getById(UUID id) {
        MatchDetailsEntity detail = matchDetailRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match detail not found with id: " + id));
        return matchDetailMapper.toResponse(detail);
    }

    @Override
    public MatchDetailResponse create(CreateMatchDetailRequest request) {
        MatchDetailsEntity detail = new MatchDetailsEntity();

        MatchesEntity match = matchRepository.findActiveById(request.getMatchId())
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + request.getMatchId()));
        detail.setMatch(match);
        detail.setIdPlayer(request.getIdPlayer());
        detail.setIdTeam(request.getIdTeam());
        detail.setMinute(request.getMinute());

        if (request.getActionId() != null) {
            ActionsEntity action = actionRepository.findActiveById(request.getActionId())
                    .orElseThrow(() -> new EntityNotFoundException("Action not found with id: " + request.getActionId()));
            detail.setAction(action);
        }

        return matchDetailMapper.toResponse(matchDetailRepository.save(detail));
    }

    @Override
    public MatchDetailResponse update(UUID id, UpdateMatchDetailRequest request) {
        MatchDetailsEntity detail = matchDetailRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match detail not found with id: " + id));

        if (request.getMatchId() != null) {
            MatchesEntity match = matchRepository.findActiveById(request.getMatchId())
                    .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + request.getMatchId()));
            detail.setMatch(match);
        }
        if (request.getIdPlayer() != null) detail.setIdPlayer(request.getIdPlayer());
        if (request.getIdTeam() != null) detail.setIdTeam(request.getIdTeam());
        if (request.getMinute() != null) detail.setMinute(request.getMinute());
        if (request.getActionId() != null) {
            ActionsEntity action = actionRepository.findActiveById(request.getActionId())
                    .orElseThrow(() -> new EntityNotFoundException("Action not found with id: " + request.getActionId()));
            detail.setAction(action);
        }

        return matchDetailMapper.toResponse(matchDetailRepository.save(detail));
    }

    @Override
    public void delete(UUID id) {
        MatchDetailsEntity detail = matchDetailRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match detail not found with id: " + id));
        detail.setDeletedAt(LocalDateTime.now());
        matchDetailRepository.delete(detail);
    }

    @Override
    public void hardDelete(UUID id) {
        MatchDetailsEntity detail = matchDetailRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match detail not found with id: " + id));
        matchDetailRepository.hardDelete(detail);
    }
}
