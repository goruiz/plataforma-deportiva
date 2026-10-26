package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IPlayerStatsService;
import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchEventType;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse.PlayerStatsResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The scorers, assisters and cards of an event, added up from the timelines of its matches. Only players with
 * something to show appear. Goals come first because it is what a leaderboard is read for.
 */
@Service
@RequiredArgsConstructor
public class PlayerStatsService implements IPlayerStatsService {

    private static final Comparator<PlayerStatsResponse> LEADERBOARD_ORDER =
            Comparator.comparingInt(PlayerStatsResponse::getGoals).reversed()
                    .thenComparing(Comparator.comparingInt(PlayerStatsResponse::getAssists).reversed())
                    .thenComparingInt(PlayerStatsResponse::getRedCards)
                    .thenComparingInt(PlayerStatsResponse::getYellowCards)
                    .thenComparing(PlayerStatsResponse::getPlayerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final IEventRepository eventRepository;
    private final IMatchRepository matchRepository;
    private final IMatchEventRepository matchEventRepository;
    private final IPlayerRepository playerRepository;
    private final ITeamRepository teamRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStatsResponse> getPlayerStatsByEventId(UUID eventId) {
        eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        List<UUID> matchIds = matchRepository.findAllActiveByEventId(eventId).stream().map(MatchesEntity::getId).toList();
        if (matchIds.isEmpty()) {
            return List.of();
        }
        List<PlayerStatsResponse> stats = countByPlayer(matchEventRepository.findAllActiveByMatchIds(matchIds));
        nameThePlayersAndTeams(stats);
        return stats.stream().sorted(LEADERBOARD_ORDER).toList();
    }

    /** One line per player; the team is the one the player played for in the event. */
    private List<PlayerStatsResponse> countByPlayer(List<MatchEventsEntity> matchEvents) {
        Map<UUID, PlayerStatsResponse> byPlayer = new LinkedHashMap<>();
        for (MatchEventsEntity matchEvent : matchEvents) {
            PlayerStatsResponse line = byPlayer.computeIfAbsent(matchEvent.getPlayerId(), playerId -> {
                PlayerStatsResponse created = new PlayerStatsResponse();
                created.setPlayerId(playerId);
                created.setTeamId(matchEvent.getTeamId());
                return created;
            });
            count(line, matchEvent.getEventType());
        }
        return List.copyOf(byPlayer.values());
    }

    private void count(PlayerStatsResponse line, String eventType) {
        if (!MatchEventType.isValid(eventType)) {
            return;
        }
        switch (MatchEventType.valueOf(eventType)) {
            case GOAL -> line.setGoals(line.getGoals() + 1);
            case ASSIST -> line.setAssists(line.getAssists() + 1);
            case YELLOW_CARD -> line.setYellowCards(line.getYellowCards() + 1);
            case RED_CARD -> line.setRedCards(line.getRedCards() + 1);
            case FOUL -> line.setFouls(line.getFouls() + 1);
        }
    }

    /** A player and a team are known to an event only by id, so their names are read once for the whole list. */
    private void nameThePlayersAndTeams(List<PlayerStatsResponse> stats) {
        Map<UUID, String> playerNames = playerRepository
                .findAllActiveByIds(stats.stream().map(PlayerStatsResponse::getPlayerId).toList()).stream()
                .collect(Collectors.toMap(PlayersEntity::getId, player -> (player.getFirstName() + " " + player.getLastName()).trim()));
        Map<UUID, String> teamNames = teamRepository
                .findAllActiveByIds(stats.stream().map(PlayerStatsResponse::getTeamId).distinct().toList()).stream()
                .collect(Collectors.toMap(TeamsEntity::getId, TeamsEntity::getName));
        stats.forEach(line -> {
            line.setPlayerName(playerNames.get(line.getPlayerId()));
            line.setTeamName(teamNames.get(line.getTeamId()));
        });
    }
}
