package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IMatchEventsService;
import com.platform.backend.modules.matches.application.mappers.MatchEventMapper;
import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchEventType;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.matches.domain.exception.MatchEventError;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchEventRequest.CreateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest.UpdateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchEventResponse.MatchEventResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CreatorAccess;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchEventsService implements IMatchEventsService {

    private static final String MATCH_FIELD = "matchId";
    private static final String TEAM_FIELD = "teamId";
    private static final String PLAYER_FIELD = "playerId";
    private static final String EVENT_TYPE_FIELD = "eventType";

    private final IMatchEventRepository matchEventRepository;
    private final IMatchRepository matchRepository;
    private final IEventRepository eventRepository;
    private final IPlayerRepository playerRepository;
    private final MatchEventMapper matchEventMapper;

    @Override
    public List<MatchEventResponse> getAll() {
        return toResponses(matchEventRepository.findAllActive());
    }

    /** The timeline of a match: in the order things happened (an event without a minute goes last). */
    @Override
    public List<MatchEventResponse> getByMatchId(UUID matchId) {
        matchRepository.findActiveById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));
        List<MatchEventsEntity> timeline = matchEventRepository.findAllActiveByMatchId(matchId).stream()
                .sorted(Comparator.comparing(MatchEventsEntity::getMinute, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MatchEventsEntity::getCreatedAt))
                .toList();
        return toResponses(timeline);
    }

    @Override
    public MatchEventResponse getById(UUID id) {
        return toResponses(List.of(findActiveOrThrow(id))).get(0);
    }

    @Override
    public MatchEventResponse create(CreateMatchEventRequest request) {
        MatchesEntity match = findMatchOrThrow(request.getMatchId());
        requireAdminOrCreatorOfEventOf(match);
        validateEvent(match, request.getTeamId(), request.getPlayerId(), request.getEventType());
        MatchEventsEntity matchEvent = matchEventMapper.toEntity(request);
        return toResponses(List.of(matchEventRepository.save(matchEvent))).get(0);
    }

    @Override
    public MatchEventResponse update(UUID id, UpdateMatchEventRequest request) {
        MatchEventsEntity matchEvent = findActiveOrThrow(id);
        MatchesEntity storedMatch = findMatchOrThrow(matchEvent.getMatchId());
        requireAdminOrCreatorOfEventOf(storedMatch);

        // The request only carries what changes: whatever it leaves out stays as it was, and the result must be valid.
        MatchesEntity match = request.getMatchId() == null ? storedMatch : findMatchOrThrow(request.getMatchId());
        if (match != storedMatch) {
            requireAdminOrCreatorOfEventOf(match);
        }
        validateEvent(match,
                valueOrStored(request.getTeamId(), matchEvent.getTeamId()),
                valueOrStored(request.getPlayerId(), matchEvent.getPlayerId()),
                valueOrStored(request.getEventType(), matchEvent.getEventType()));

        matchEventMapper.updateEntity(matchEvent, request);
        return toResponses(List.of(matchEventRepository.save(matchEvent))).get(0);
    }

    @Override
    public void delete(UUID id) {
        MatchEventsEntity matchEvent = findActiveOrThrow(id);
        requireAdminOrCreatorOfEventOf(findMatchOrThrow(matchEvent.getMatchId()));
        matchEvent.setDeletedAt(LocalDateTime.now());
        matchEventRepository.delete(matchEvent);
    }

    @Override
    public void hardDelete(UUID id) {
        MatchEventsEntity matchEvent = matchEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match event not found with id: " + id));
        requireAdminOrCreatorOfEventOf(findMatchOrThrow(matchEvent.getMatchId()));
        matchEventRepository.hardDelete(matchEvent);
    }

    private MatchEventsEntity findActiveOrThrow(UUID id) {
        return matchEventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match event not found with id: " + id));
    }

    // The match is a plain id, so nothing else checks it: it must exist (and, for a restricted caller, be theirs).
    private MatchesEntity findMatchOrThrow(UUID matchId) {
        return matchRepository.findActiveById(matchId)
                .orElseThrow(() -> new FieldValidationException(MATCH_FIELD, MatchError.MATCH_NOT_FOUND.name()));
    }

    /** A match's timeline is kept by whoever manages its event. */
    private void requireAdminOrCreatorOfEventOf(MatchesEntity match) {
        CreatorAccess.requireAdminOrCreator(match.getEventId(), eventRepository::findActiveById);
    }

    private void validateEvent(MatchesEntity match, UUID teamId, UUID playerId, String eventType) {
        if (!MatchEventType.isValid(eventType)) {
            throw new FieldValidationException(EVENT_TYPE_FIELD, MatchEventError.INVALID_EVENT_TYPE.name());
        }
        if (!teamId.equals(match.getHomeTeamId()) && !teamId.equals(match.getAwayTeamId())) {
            throw new FieldValidationException(TEAM_FIELD, MatchEventError.TEAM_NOT_IN_MATCH.name());
        }
        boolean playerIsInTheTeam = playerRepository.findActiveById(playerId)
                .map(PlayersEntity::getTeam)
                .filter(team -> teamId.equals(team.getId()))
                .isPresent();
        if (!playerIsInTheTeam) {
            throw new FieldValidationException(PLAYER_FIELD, MatchEventError.PLAYER_NOT_IN_TEAM.name());
        }
    }

    private <T> T valueOrStored(T requested, T stored) {
        return requested != null ? requested : stored;
    }

    /** A line of the timeline says who did it, which the event only knows by id: the names are read once per call. */
    private List<MatchEventResponse> toResponses(List<MatchEventsEntity> matchEvents) {
        List<UUID> playerIds = matchEvents.stream().map(MatchEventsEntity::getPlayerId).distinct().toList();
        Map<UUID, String> playerNames = playerIds.isEmpty()
                ? Map.of()
                : playerRepository.findAllActiveByIds(playerIds).stream()
                        .collect(Collectors.toMap(PlayersEntity::getId, this::fullNameOf, (first, second) -> first));
        return matchEvents.stream().map(matchEvent -> {
            MatchEventResponse response = matchEventMapper.toResponse(matchEvent);
            response.setPlayerName(playerNames.get(matchEvent.getPlayerId()));
            return response;
        }).toList();
    }

    private String fullNameOf(PlayersEntity player) {
        return (player.getFirstName() + " " + player.getLastName()).trim();
    }
}
