package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchTimingService;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStatus;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest.CreateMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.RescheduleDateRequest.RescheduleDateRequest;
import com.platform.backend.modules.matches.presentation.requests.RescheduleMatchRequest.RescheduleMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchesService implements IMatchesService {

    private static final String HOME_SCORE_FIELD = "homeScore";
    private static final String AWAY_SCORE_FIELD = "awayScore";

    private final IMatchRepository matchRepository;
    private final IMatchTimingService matchTimingService;
    private final MatchMapper matchMapper;

    @Override
    public List<MatchResponse> getAll() {
        return toResponses(matchRepository.findAllActive());
    }

    @Override
    public List<MatchResponse> getByEventId(UUID eventId) {
        return toResponses(matchRepository.findAllActiveByEventId(eventId));
    }

    @Override
    public MatchResponse getById(UUID id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Override
    public MatchResponse create(CreateMatchRequest request) {
        MatchesEntity match = matchMapper.toEntity(request);
        return toResponse(matchRepository.save(match));
    }

    /**
     * Once a match has started only its status and its result can change. A result (a score) can only be recorded
     * once the match is over; recording one finishes the match, unless the request says otherwise, so nobody has to
     * mark it as finished by hand.
     */
    @Override
    public MatchResponse update(UUID id, UpdateMatchRequest request) {
        MatchesEntity match = findActiveOrThrow(id);
        validateDataIsStillEditable(match, request);
        matchMapper.updateEntity(match, request);

        if (isRecordingResult(request)) {
            validateMatchHasEnded(match, request);
            finishIfStillOpen(match, request);
        }
        return toResponse(matchRepository.save(match));
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

        return toResponses(matches);
    }

    @Override
    public MatchResponse postpone(UUID id) {
        MatchesEntity match = findActiveOrThrow(id);

        if (Set.of("COMPLETED", "SUSPENDED").contains(match.getStatus())) {
            throw new IllegalStateException(
                "Cannot postpone a match with status: " + match.getStatus());
        }

        match.setStatus("POSTPONED");
        return toResponse(matchRepository.save(match));
    }

    @Override
    public MatchResponse suspend(UUID id) {
        MatchesEntity match = findActiveOrThrow(id);

        if ("COMPLETED".equals(match.getStatus())) {
            throw new IllegalStateException("Cannot suspend a completed match.");
        }

        match.setStatus("SUSPENDED");
        return toResponse(matchRepository.save(match));
    }

    @Override
    public MatchResponse rescheduleMatch(UUID id, RescheduleMatchRequest request) {
        MatchesEntity match = findActiveOrThrow(id);

        if (!Set.of("POSTPONED", "SUSPENDED").contains(match.getStatus())) {
            throw new IllegalStateException(
                "Only POSTPONED or SUSPENDED matches can be rescheduled. Current status: " + match.getStatus());
        }

        match.setMatchDate(request.getNewDate());
        match.setStatus("RESCHEDULED");
        return toResponse(matchRepository.save(match));
    }

    @Override
    @Transactional
    public int finishEndedMatches() {
        LocalDateTime now = LocalDateTime.now();
        return finishEnded(
                matchRepository.findAllActiveByStatusInAndStartedBy(MatchStatus.autoFinishableNames(), now), now);
    }

    @Override
    @Transactional
    public int finishEndedMatches(UUID eventId) {
        List<MatchesEntity> candidates = matchRepository.findAllActiveByEventId(eventId).stream()
                .filter(match -> MatchStatus.canFinishAutomatically(match.getStatus()))
                .toList();
        return finishEnded(candidates, LocalDateTime.now());
    }

    @Override
    public void delete(UUID id) {
        MatchesEntity match = findActiveOrThrow(id);
        match.setDeletedAt(LocalDateTime.now());
        matchRepository.delete(match);
    }

    @Override
    public void hardDelete(UUID id) {
        MatchesEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        matchRepository.hardDelete(match);
    }

    private MatchesEntity findActiveOrThrow(UUID id) {
        return matchRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
    }

    private int finishEnded(List<MatchesEntity> candidates, LocalDateTime now) {
        Map<UUID, LocalDateTime> endTimes = matchTimingService.getEndTimes(candidates);
        List<MatchesEntity> ended = candidates.stream()
                .filter(match -> !endTimes.get(match.getId()).isAfter(now))
                .toList();

        for (MatchesEntity match : ended) {
            match.setStatus(MatchStatus.FINISHED.name());
            matchRepository.save(match);
        }
        return ended.size();
    }

    /** Checked before the mapper overwrites the stored values: a value sent unchanged (as the mobile app does) is fine. */
    private void validateDataIsStillEditable(MatchesEntity match, UpdateMatchRequest request) {
        if (!hasStarted(match, LocalDateTime.now())) {
            return;
        }
        requestedDataChanges(match, request).entrySet().stream()
                .filter(Map.Entry::getValue)
                .findFirst()
                .ifPresent(changed -> {
                    throw new FieldValidationException(changed.getKey(), MatchError.MATCH_ALREADY_STARTED.name());
                });
    }

    /** For each data field the request may change: whether it asks for a value different from the stored one. */
    private Map<String, Boolean> requestedDataChanges(MatchesEntity match, UpdateMatchRequest request) {
        Map<String, Boolean> changes = new LinkedHashMap<>();
        changes.put("homeTeamId", isChange(request.getHomeTeamId(), match.getHomeTeamId()));
        changes.put("awayTeamId", isChange(request.getAwayTeamId(), match.getAwayTeamId()));
        changes.put("matchDate", isChange(request.getMatchDate(), match.getMatchDate()));
        changes.put("location", isChange(request.getLocation(), match.getLocation()));
        changes.put("courtId", isChange(request.getCourtId(), match.getCourtId()));
        changes.put("eventId", isChange(request.getEventId(), match.getEventId()));
        return changes;
    }

    /** A null means "leave as is" in an update, so only a value that differs is a change. */
    private boolean isChange(Object requested, Object stored) {
        return requested != null && !Objects.equals(requested, stored);
    }

    /**
     * A match has started once its kick-off time has passed. A postponed one never was played, so its data can still
     * be corrected until it is rescheduled.
     */
    private boolean hasStarted(MatchesEntity match, LocalDateTime now) {
        return !match.getMatchDate().isAfter(now) && !MatchStatus.POSTPONED.name().equals(match.getStatus());
    }

    private boolean isRecordingResult(UpdateMatchRequest request) {
        return request.getHomeScore() != null || request.getAwayScore() != null;
    }

    private void validateMatchHasEnded(MatchesEntity match, UpdateMatchRequest request) {
        if (!matchTimingService.hasEnded(match, LocalDateTime.now())) {
            String scoreField = request.getHomeScore() != null ? HOME_SCORE_FIELD : AWAY_SCORE_FIELD;
            throw new FieldValidationException(scoreField, MatchError.RESULT_BEFORE_MATCH_END.name());
        }
    }

    /** A status the request sets explicitly is respected; otherwise a match that was still open is finished. */
    private void finishIfStillOpen(MatchesEntity match, UpdateMatchRequest request) {
        if (request.getStatus() == null && MatchStatus.canFinishAutomatically(match.getStatus())) {
            match.setStatus(MatchStatus.FINISHED.name());
        }
    }

    private MatchResponse toResponse(MatchesEntity match) {
        return toResponses(List.of(match)).get(0);
    }

    /** Every response carries when its match ends, which needs each event's schedule config (read once per event). */
    private List<MatchResponse> toResponses(List<MatchesEntity> matches) {
        Map<UUID, LocalDateTime> endTimes = matchTimingService.getEndTimes(matches);
        return matches.stream().map(match -> {
            MatchResponse response = matchMapper.toResponse(match);
            response.setEndsAt(endTimes.get(match.getId()));
            return response;
        }).toList();
    }
}
