package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IPlayerStatsService;
import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse.PlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.PlayerTotalsResponse.PlayerTotalsResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The scorers, assisters and cards of an event, added up from the timelines of its matches. Only players with
 * something to show appear. Goals come first because it is what a leaderboard is read for. Also the totals of one
 * player across every event.
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

        List<MatchesEntity> matches = matchRepository.findAllActiveByEventId(eventId);
        if (matches.isEmpty()) {
            return List.of();
        }
        List<UUID> matchIds = matches.stream().map(MatchesEntity::getId).toList();
        List<PlayerStatsResponse> stats = countByPlayer(matchEventRepository.findAllActiveByMatchIds(matchIds));
        nameThePlayersAndTeams(stats);
        stats.forEach(line -> line.setMatchesPlayed(countPlayedMatchesOfTeam(line.getTeamId(), matches)));
        return stats.stream().sorted(LEADERBOARD_ORDER).toList();
    }

    /** Only a player the caller can see has totals to read: any other is a 404, like the player itself. */
    @Override
    @Transactional(readOnly = true)
    public PlayerTotalsResponse getPlayerTotals(UUID playerId) {
        PlayersEntity player = playerRepository.findActiveById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + playerId));
        return totalsOf(player);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerTotalsResponse getCurrentPlayerTotals() {
        String email = EmailNormalizer.normalize(CurrentCaller.require().getEmail());
        PlayersEntity player = playerRepository.findByEmail(email)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));
        return totalsOf(player);
    }

    /** One line per player; the team is the one the player played for in the event. */
    private List<PlayerStatsResponse> countByPlayer(List<MatchEventsEntity> matchEvents) {
        Map<UUID, List<MatchEventsEntity>> eventsByPlayer = matchEvents.stream()
                .collect(Collectors.groupingBy(MatchEventsEntity::getPlayerId, LinkedHashMap::new, Collectors.toList()));
        return eventsByPlayer.entrySet().stream()
                .map(entry -> toLine(entry.getKey(), entry.getValue()))
                .toList();
    }

    private PlayerStatsResponse toLine(UUID playerId, List<MatchEventsEntity> playerEvents) {
        PlayerEventTally tally = tallyOf(playerEvents);
        PlayerStatsResponse line = new PlayerStatsResponse();
        line.setPlayerId(playerId);
        line.setTeamId(playerEvents.get(0).getTeamId());
        line.setGoals(tally.goals());
        line.setAssists(tally.assists());
        line.setYellowCards(tally.yellowCards());
        line.setRedCards(tally.redCards());
        line.setFouls(tally.fouls());
        return line;
    }

    private static PlayerEventTally tallyOf(List<MatchEventsEntity> playerEvents) {
        PlayerEventTally tally = new PlayerEventTally();
        playerEvents.forEach(playerEvent -> tally.add(playerEvent.getEventType()));
        return tally;
    }

    private static int countPlayedMatchesOfTeam(UUID teamId, List<MatchesEntity> matches) {
        return (int) matches.stream().filter(match -> match.hasResult() && match.isPlayedBy(teamId)).count();
    }

    /** A player and a team are known to an event only by id, so their names are read once for the whole list. */
    private void nameThePlayersAndTeams(List<PlayerStatsResponse> stats) {
        // Names only, and without the organization filter: a leaderboard of a public event names players of another organization.
        Map<UUID, String> playerNames = playerRepository
                .findFullNamesByIds(stats.stream().map(PlayerStatsResponse::getPlayerId).toList());
        Map<UUID, String> teamNames = teamRepository
                .findAllActiveByIds(stats.stream().map(PlayerStatsResponse::getTeamId).distinct().toList()).stream()
                .collect(Collectors.toMap(TeamsEntity::getId, TeamsEntity::getName));
        stats.forEach(line -> {
            line.setPlayerName(playerNames.get(line.getPlayerId()));
            line.setTeamName(teamNames.get(line.getTeamId()));
        });
    }

    private PlayerTotalsResponse totalsOf(PlayersEntity player) {
        List<MatchEventsEntity> playerEvents = matchEventRepository.findAllActiveByPlayerId(player.getId());
        PlayerEventTally tally = tallyOf(playerEvents);

        PlayerTotalsResponse totals = new PlayerTotalsResponse();
        totals.setPlayerId(player.getId());
        totals.setGoals(tally.goals());
        totals.setAssists(tally.assists());
        totals.setYellowCards(tally.yellowCards());
        totals.setRedCards(tally.redCards());
        totals.setFouls(tally.fouls());
        totals.setMatchesPlayed(countMatchesPlayed(player, playerEvents));
        return totals;
    }

    /**
     * The played matches of the player's current team, joined with the matches where the player has an event (a goal
     * for a team they left), each counted once. Nothing records when a player joined their team, so this is the closest
     * the data allows.
     */
    private int countMatchesPlayed(PlayersEntity player, List<MatchEventsEntity> playerEvents) {
        Set<UUID> matchIds = new HashSet<>();
        playerEvents.forEach(playerEvent -> matchIds.add(playerEvent.getMatchId()));
        if (player.getTeam() != null) {
            matchRepository.findAllActiveByTeamId(player.getTeam().getId()).stream()
                    .filter(MatchesEntity::hasResult)
                    .forEach(match -> matchIds.add(match.getId()));
        }
        return matchIds.size();
    }
}
