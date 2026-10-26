package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceTest {

    @Mock private IEventRepository eventRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IMatchEventRepository matchEventRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;

    @InjectMocks
    private PlayerStatsService playerStatsService;

    private final UUID eventId = UUID.randomUUID();
    private final MatchesEntity match = matchWithId();
    private TeamsEntity tigres;
    private TeamsEntity leones;
    private PlayersEntity ana;
    private PlayersEntity luis;
    private PlayersEntity eva;

    @BeforeEach
    void setUp() {
        tigres = team("Tigres");
        leones = team("Leones");
        ana = player("Ana", "Lopez");
        luis = player("Luis", "Mora");
        eva = player("Eva", "Rios");

        lenient().when(eventRepository.findActiveById(eventId)).thenReturn(Optional.of(new EventsEntity()));
        lenient().when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(match));
        lenient().when(playerRepository.findAllActiveByIds(any())).thenReturn(List.of(ana, luis, eva));
        lenient().when(teamRepository.findAllActiveByIds(any())).thenReturn(List.of(tigres, leones));
    }

    private static MatchesEntity matchWithId() {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        return match;
    }

    private static TeamsEntity team(String name) {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName(name);
        return team;
    }

    private static PlayersEntity player(String firstName, String lastName) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setFirstName(firstName);
        player.setLastName(lastName);
        return player;
    }

    private MatchEventsEntity event(PlayersEntity player, TeamsEntity team, MatchEventType type) {
        MatchEventsEntity event = new MatchEventsEntity();
        event.setMatchId(match.getId());
        event.setPlayerId(player.getId());
        event.setTeamId(team.getId());
        event.setEventType(type.name());
        return event;
    }

    private void givenTimeline(MatchEventsEntity... events) {
        when(matchEventRepository.findAllActiveByMatchIds(List.of(match.getId()))).thenReturn(List.of(events));
    }

    @Test
    void getPlayerStatsByEventId_whenPlayersScoredAndWereCarded_thenEachLineAddsUpItsOwnEvents() {
        givenTimeline(
                event(ana, tigres, MatchEventType.GOAL), event(ana, tigres, MatchEventType.GOAL),
                event(ana, tigres, MatchEventType.YELLOW_CARD), event(luis, leones, MatchEventType.ASSIST),
                event(luis, leones, MatchEventType.RED_CARD), event(luis, leones, MatchEventType.FOUL));

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        PlayerStatsResponse anaLine = stats.get(0);
        assertThat(anaLine.getPlayerName()).isEqualTo("Ana Lopez");
        assertThat(anaLine.getTeamName()).isEqualTo("Tigres");
        assertThat(anaLine.getGoals()).isEqualTo(2);
        assertThat(anaLine.getYellowCards()).isEqualTo(1);
        PlayerStatsResponse luisLine = stats.get(1);
        assertThat(luisLine.getAssists()).isEqualTo(1);
        assertThat(luisLine.getRedCards()).isEqualTo(1);
        assertThat(luisLine.getFouls()).isEqualTo(1);
        assertThat(luisLine.getGoals()).isZero();
    }

    @Test
    void getPlayerStatsByEventId_whenSeveralPlayers_thenMostGoalsFirstThenAssistsThenFewerCardsThenName() {
        givenTimeline(
                event(eva, tigres, MatchEventType.GOAL), event(luis, leones, MatchEventType.GOAL),
                event(ana, tigres, MatchEventType.GOAL), event(ana, tigres, MatchEventType.GOAL),
                event(luis, leones, MatchEventType.ASSIST), event(eva, tigres, MatchEventType.YELLOW_CARD));

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        // Ana 2 goals; Luis and Eva 1 goal each, but Luis also assisted.
        assertThat(stats).extracting(PlayerStatsResponse::getPlayerName).containsExactly("Ana Lopez", "Luis Mora", "Eva Rios");
    }

    @Test
    void getPlayerStatsByEventId_whenAnEventTypeIsNotKnown_thenItIsIgnoredButThePlayerStillAppears() {
        MatchEventsEntity unknown = event(ana, tigres, MatchEventType.GOAL);
        unknown.setEventType("SOMETHING_ELSE");
        givenTimeline(unknown);

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        assertThat(stats).singleElement().satisfies(line -> assertThat(line.getGoals()).isZero());
    }

    @Test
    void getPlayerStatsByEventId_whenTheEventHasNoMatches_thenThereIsNothingToCount() {
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of());

        assertThat(playerStatsService.getPlayerStatsByEventId(eventId)).isEmpty();
        verifyNoInteractions(matchEventRepository);
    }

    @Test
    void getPlayerStatsByEventId_whenTheEventDoesNotExist_thenNotFound() {
        assertThatThrownBy(() -> playerStatsService.getPlayerStatsByEventId(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
