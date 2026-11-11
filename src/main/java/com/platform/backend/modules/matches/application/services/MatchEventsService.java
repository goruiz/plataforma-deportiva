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
import com.platform.backend.modules.players.application.services.PlayerEligibilityPolicy;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CreatorAccess;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MatchEventsService implements IMatchEventsService {

    private static final String MATCH_FIELD = "matchId";
    private static final String TEAM_FIELD = "teamId";
    private static final String PLAYER_FIELD = "playerId";
    private static final String EVENT_TYPE_FIELD = "eventType";
    private static final String REPLACED_PLAYER_FIELD = "replacedPlayerId";

    private final IMatchEventRepository matchEventRepository;
    private final IMatchRepository matchRepository;
    private final IEventRepository eventRepository;
    private final IPlayerRepository playerRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final ITeamRepository teamRepository;
    private final PlayerEligibilityPolicy eligibilityPolicy;
    private final MatchSettingsReader settingsReader;
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
        validateEvent(match, request.getTeamId(), request.getPlayerId(), request.getEventType(),
                request.getReplacedPlayerId(), null);
        MatchEventsEntity matchEvent = matchEventMapper.toEntity(request);
        clearReplacedPlayerUnlessSubstitution(matchEvent);
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
                valueOrStored(request.getEventType(), matchEvent.getEventType()),
                valueOrStored(request.getReplacedPlayerId(), matchEvent.getReplacedPlayerId()),
                matchEvent.getId());

        matchEventMapper.updateEntity(matchEvent, request);
        clearReplacedPlayerUnlessSubstitution(matchEvent);
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

    /** {@code ownEventId} is the event being changed (null for a new one): it does not count against the substitution limit. */
    private void validateEvent(MatchesEntity match, UUID teamId, UUID playerId, String eventType, UUID replacedPlayerId,
                               UUID ownEventId) {
        if (!MatchEventType.isValid(eventType)) {
            throw new FieldValidationException(EVENT_TYPE_FIELD, MatchEventError.INVALID_EVENT_TYPE.name());
        }
        if (!teamId.equals(match.getHomeTeamId()) && !teamId.equals(match.getAwayTeamId())) {
            throw new FieldValidationException(TEAM_FIELD, MatchEventError.TEAM_NOT_IN_MATCH.name());
        }
        if (!teamMemberRepository.existsActiveByPlayerIdAndTeamId(playerId, teamId)) {
            throw new FieldValidationException(PLAYER_FIELD, MatchEventError.PLAYER_NOT_IN_TEAM.name());
        }
        requirePlayerMeetsCategoryOf(teamId, playerId);
        if (MatchEventType.SUBSTITUTION.name().equals(eventType)) {
            validateSubstitution(match, teamId, playerId, replacedPlayerId, ownEventId);
        }
    }

    /**
     * A player the category of the team does not admit (its gender, its ages) may not play, so nothing is recorded for
     * them: the reason is the code of the refusal. A team the caller cannot see has no rules to apply here.
     */
    private void requirePlayerMeetsCategoryOf(UUID teamId, UUID playerId) {
        teamRepository.findActiveById(teamId).ifPresent(team ->
                playerRepository.findActiveById(playerId)
                        .flatMap(player -> eligibilityPolicy.reasonWhyNot(player, team))
                        .ifPresent(reason -> {
                            throw new FieldValidationException(PLAYER_FIELD, reason.name());
                        }));
    }

    /** Says who goes out (a player of the same team, other than the one who comes in) and respects the limit of the team. */
    private void validateSubstitution(MatchesEntity match, UUID teamId, UUID playerId, UUID replacedPlayerId,
                                      UUID ownEventId) {
        if (replacedPlayerId == null) {
            throw new FieldValidationException(REPLACED_PLAYER_FIELD, MatchEventError.REPLACED_PLAYER_REQUIRED.name());
        }
        if (replacedPlayerId.equals(playerId)) {
            throw new FieldValidationException(REPLACED_PLAYER_FIELD, MatchEventError.REPLACED_PLAYER_SAME_AS_PLAYER.name());
        }
        if (!teamMemberRepository.existsActiveByPlayerIdAndTeamId(replacedPlayerId, teamId)) {
            throw new FieldValidationException(REPLACED_PLAYER_FIELD, MatchEventError.PLAYER_NOT_IN_TEAM.name());
        }
        int allowed = settingsReader.of(match).maxSubstitutions();
        if (countSubstitutionsOf(match.getId(), teamId, ownEventId) >= allowed) {
            throw new FieldValidationException(EVENT_TYPE_FIELD, MatchEventError.SUBSTITUTION_LIMIT_REACHED.name());
        }
    }

    private long countSubstitutionsOf(UUID matchId, UUID teamId, UUID ownEventId) {
        return matchEventRepository.findAllActiveByMatchId(matchId).stream()
                .filter(event -> MatchEventType.SUBSTITUTION.name().equals(event.getEventType()))
                .filter(event -> teamId.equals(event.getTeamId()))
                .filter(event -> !Objects.equals(event.getId(), ownEventId))
                .count();
    }

    /** Who goes out only means something in a substitution. */
    private void clearReplacedPlayerUnlessSubstitution(MatchEventsEntity matchEvent) {
        if (!MatchEventType.SUBSTITUTION.name().equals(matchEvent.getEventType())) {
            matchEvent.setReplacedPlayerId(null);
        }
    }

    private <T> T valueOrStored(T requested, T stored) {
        return requested != null ? requested : stored;
    }

    /** A line of the timeline says who did it, which the event only knows by id: the names are read once per call. */
    private List<MatchEventResponse> toResponses(List<MatchEventsEntity> matchEvents) {
        List<UUID> playerIds = matchEvents.stream()
                .flatMap(event -> Stream.of(event.getPlayerId(), event.getReplacedPlayerId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // Names only, and without the organization filter: the timeline of a public event names players of another organization.
        Map<UUID, String> playerNames = playerRepository.findFullNamesByIds(playerIds);
        return matchEvents.stream().map(matchEvent -> {
            MatchEventResponse response = matchEventMapper.toResponse(matchEvent);
            response.setPlayerName(playerNames.get(matchEvent.getPlayerId()));
            if (matchEvent.getReplacedPlayerId() != null) {
                response.setReplacedPlayerName(playerNames.get(matchEvent.getReplacedPlayerId()));
            }
            return response;
        }).toList();
    }
}
