package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.services.TeamStatsCalculator.EventTotals;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchSide;
import com.platform.backend.modules.matches.domain.enums.TeamMatchResult;
import com.platform.backend.modules.matches.domain.models.GroupedCount;
import com.platform.backend.modules.matches.domain.models.TeamMatchTally;
import com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse.TeamMatchHistoryResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse.TeamPlayerStatsResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamStatsCalculatorTest {

    private final UUID tigres = UUID.randomUUID();
    private final UUID leones = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    // ─── result of a match for a team ───────────────────────────────────────────

    @Test
    void theResultIsReadFromTheTeamsPointOfView() {
        assertThat(TeamMatchResult.of(3, 1)).isEqualTo(TeamMatchResult.WIN);
        assertThat(TeamMatchResult.of(2, 2)).isEqualTo(TeamMatchResult.DRAW);
        assertThat(TeamMatchResult.of(0, 1)).isEqualTo(TeamMatchResult.LOSS);
    }

    // ─── the two sides of a team ────────────────────────────────────────────────

    @Test
    void mergeSides_addsTheHomeAndAwayMatchesOfTheSameTeam() {
        Map<UUID, TeamMatchTally> merged = TeamStatsCalculator.mergeSides(
                List.of(new TeamMatchTally(tigres, 2, 1, 1, 0, 5, 3)),
                List.of(new TeamMatchTally(tigres, 1, 0, 0, 1, 0, 2), new TeamMatchTally(leones, 1, 1, 0, 0, 4, 0)));

        assertThat(merged.get(tigres)).isEqualTo(new TeamMatchTally(tigres, 3, 1, 1, 1, 5, 5));
        assertThat(merged.get(leones)).isEqualTo(new TeamMatchTally(leones, 1, 1, 0, 0, 4, 0));
    }

    @Test
    void mergeSides_withNoMatchesIsEmpty() {
        assertThat(TeamStatsCalculator.mergeSides(List.of(), List.of())).isEmpty();
    }

    // ─── goals, cards and fouls ─────────────────────────────────────────────────

    @Test
    void sumEventsByGroup_foldsEachGroupsTypesIntoOneLine() {
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();

        Map<UUID, EventTotals> totals = TeamStatsCalculator.sumEventsByGroup(List.of(
                new GroupedCount(ana, "GOAL", 3), new GroupedCount(ana, "YELLOW_CARD", 2), new GroupedCount(ana, "FOUL", 4),
                new GroupedCount(luis, "ASSIST", 1), new GroupedCount(luis, "RED_CARD", 1)));

        assertThat(totals.get(ana)).isEqualTo(new EventTotals(3, 0, 2, 0, 4));
        assertThat(totals.get(luis)).isEqualTo(new EventTotals(0, 1, 0, 1, 0));
    }

    @Test
    void sumEventsByGroup_ignoresATypeItDoesNotKnow() {
        UUID ana = UUID.randomUUID();

        Map<UUID, EventTotals> totals = TeamStatsCalculator.sumEventsByGroup(List.of(
                new GroupedCount(ana, "OFFSIDE", 5), new GroupedCount(ana, null, 1), new GroupedCount(ana, "GOAL", 1)));

        assertThat(totals.get(ana)).isEqualTo(new EventTotals(1, 0, 0, 0, 0));
    }

    // ─── a line of the history ──────────────────────────────────────────────────

    private MatchesEntity match(UUID home, UUID away, Integer homeScore, Integer awayScore) {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setHomeTeamId(home);
        match.setAwayTeamId(away);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setEventId(eventId);
        match.setMatchDate(LocalDateTime.of(2026, 11, 1, 18, 0));
        match.setStatus("FINISHED");
        return match;
    }

    @Test
    void historyLine_atHomeTheTeamsGoalsAreTheHomeScore() {
        TeamMatchHistoryResponse line = TeamStatsCalculator.historyLine(match(tigres, leones, 3, 1), tigres,
                Map.of(eventId, "Liga Norte"), Map.of(leones, "Leones"));

        assertThat(line.getSide()).isEqualTo(MatchSide.HOME);
        assertThat(line.getOpponentId()).isEqualTo(leones);
        assertThat(line.getOpponentName()).isEqualTo("Leones");
        assertThat(line.getEventName()).isEqualTo("Liga Norte");
        assertThat(line.getGoalsFor()).isEqualTo(3);
        assertThat(line.getGoalsAgainst()).isEqualTo(1);
        assertThat(line.getResult()).isEqualTo(TeamMatchResult.WIN);
        assertThat(line.getStatus()).isEqualTo("FINISHED");
    }

    @Test
    void historyLine_awayTheTeamsGoalsAreTheAwayScore() {
        TeamMatchHistoryResponse line = TeamStatsCalculator.historyLine(match(leones, tigres, 3, 1), tigres,
                Map.of(), Map.of(leones, "Leones"));

        assertThat(line.getSide()).isEqualTo(MatchSide.AWAY);
        assertThat(line.getOpponentId()).isEqualTo(leones);
        assertThat(line.getGoalsFor()).isEqualTo(1);
        assertThat(line.getGoalsAgainst()).isEqualTo(3);
        assertThat(line.getResult()).isEqualTo(TeamMatchResult.LOSS);
        assertThat(line.getEventName()).isNull();
    }

    @Test
    void historyLine_aLevelMatchIsADraw() {
        TeamMatchHistoryResponse line = TeamStatsCalculator.historyLine(match(tigres, leones, 2, 2), tigres, Map.of(), Map.of());

        assertThat(line.getResult()).isEqualTo(TeamMatchResult.DRAW);
        assertThat(line.getOpponentName()).isNull();
    }

    @Test
    void historyLine_withoutAResultThereAreNoGoalsAndNoOutcome() {
        TeamMatchHistoryResponse line = TeamStatsCalculator.historyLine(match(tigres, leones, null, null), tigres,
                Map.of(), Map.of());

        assertThat(line.getGoalsFor()).isNull();
        assertThat(line.getGoalsAgainst()).isNull();
        assertThat(line.getResult()).isNull();
    }

    @Test
    void historyLine_aMatchWithoutAnEventHasNoEventName() {
        MatchesEntity withoutEvent = match(tigres, leones, 1, 0);
        withoutEvent.setEventId(null);

        assertThat(TeamStatsCalculator.historyLine(withoutEvent, tigres, Map.of(eventId, "Liga"), Map.of()).getEventName()).isNull();
    }

    // ─── ranking of the players ─────────────────────────────────────────────────

    private TeamPlayerStatsResponse line(String name, int goals, int assists, int yellow, int red) {
        TeamPlayerStatsResponse line = new TeamPlayerStatsResponse();
        line.setPlayerName(name);
        line.setGoals(goals);
        line.setAssists(assists);
        line.setYellowCards(yellow);
        line.setRedCards(red);
        return line;
    }

    @Test
    void theRankingOrdersByGoalsThenAssistsThenFewerCardsThenName() {
        List<TeamPlayerStatsResponse> ranking = new ArrayList<>(List.of(
                line("Zoe", 1, 0, 0, 0),
                line("Beto", 2, 1, 0, 0),
                line("Ana", 2, 1, 1, 0),
                line("Carla", 2, 3, 0, 0),
                line("Dani", 2, 1, 0, 0),
                line(null, 0, 0, 0, 0)));

        ranking.sort(TeamStatsCalculator.PLAYER_RANKING_ORDER);

        assertThat(ranking).extracting(TeamPlayerStatsResponse::getPlayerName)
                .containsExactly("Carla", "Beto", "Dani", "Ana", "Zoe", null);
    }
}
