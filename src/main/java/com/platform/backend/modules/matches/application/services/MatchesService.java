package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest.CreateMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.RescheduleDateRequest.RescheduleDateRequest;
import com.platform.backend.modules.matches.presentation.requests.RescheduleMatchRequest.RescheduleMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
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
    public List<MatchResponse> getByEventId(UUID eventId) {
        return matchRepository.findAllActiveByEventId(eventId)
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
    @Transactional
    public List<MatchResponse> rescheduleDate(UUID eventId, RescheduleDateRequest request) {
        List<MatchesEntity> matches = matchRepository.findAllActiveByEventIdAndDate(eventId, request.getFromDate());

        if (matches.isEmpty()) {
            throw new EntityNotFoundException(
                    "No matches found for event " + eventId + " on " + request.getFromDate());
        }

        LocalDate toDate = request.getToDate();
        for (MatchesEntity match : matches) {
            if (toDate != null) {
                LocalTime time = match.getMatchDate().toLocalTime();
                match.setMatchDate(LocalDateTime.of(toDate, time));
                if ("POSTPONED".equals(match.getStatus())) {
                    match.setStatus("SCHEDULED");
                }
            } else {
                match.setStatus("POSTPONED");
            }
            matchRepository.save(match);
        }

        return matches.stream().map(matchMapper::toResponse).toList();
    }

    @Override
    public MatchResponse postpone(UUID id) {
        MatchesEntity match = matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));

        if (Set.of("COMPLETED", "SUSPENDED").contains(match.getStatus())) {
            throw new IllegalStateException(
                "Cannot postpone a match with status: " + match.getStatus());
        }

        match.setStatus("POSTPONED");
        return matchMapper.toResponse(matchRepository.save(match));
    }

    @Override
    public MatchResponse suspend(UUID id) {
        MatchesEntity match = matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));

        if ("COMPLETED".equals(match.getStatus())) {
            throw new IllegalStateException("Cannot suspend a completed match.");
        }

        match.setStatus("SUSPENDED");
        return matchMapper.toResponse(matchRepository.save(match));
    }

    @Override
    public MatchResponse rescheduleMatch(UUID id, RescheduleMatchRequest request) {
        MatchesEntity match = matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));

        if (!Set.of("POSTPONED", "SUSPENDED").contains(match.getStatus())) {
            throw new IllegalStateException(
                "Only POSTPONED or SUSPENDED matches can be rescheduled. Current status: " + match.getStatus());
        }

        match.setMatchDate(request.getNewDate());
        match.setStatus("RESCHEDULED");
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
