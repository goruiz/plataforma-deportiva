package com.platform.backend.modules.matches.application.mappers;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.matches.application.engine.ChampionshipProgress;
import com.platform.backend.modules.matches.application.engine.ChampionshipRules;
import com.platform.backend.modules.matches.application.engine.MatchRef;
import com.platform.backend.modules.matches.application.engine.StandingRow;
import com.platform.backend.modules.matches.application.engine.TeamRef;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.presentation.responses.ChampionshipResponse.ChampionshipResponse;
import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Turns what the championship engine worked out into the response clients receive (team ids come with their names). */
@Component
public class ChampionshipResponseMapper {

    /** An event whose format does not tell how it is played (none, or one without rules) has no championship to run. */
    public ChampionshipResponse toResponseWithoutChampionship(EventsEntity event, List<TeamRef> teams) {
        ChampionshipResponse response = baseResponse(event, teams, false);
        ChampionshipResponse.NextStepResponse nextStep = new ChampionshipResponse.NextStepResponse();
        nextStep.setAction(ChampionshipAction.NO_FORMAT);
        nextStep.setRegisteredTeams(teams.size());
        nextStep.setMatchesToResolve(List.of());
        response.setNextStep(nextStep);
        response.setPhases(List.of());
        response.setTables(List.of());
        response.setRounds(List.of());
        return response;
    }

    public ChampionshipResponse toResponse(EventsEntity event, ChampionshipRules rules, ChampionshipProgress progress,
                                           List<TeamRef> teams, boolean scheduleConfigured) {
        Map<UUID, String> teamNames = teams.stream().collect(Collectors.toMap(TeamRef::id, TeamRef::name));
        ChampionshipResponse response = baseResponse(event, teams, scheduleConfigured);
        response.setStructure(rules.structure());
        response.setGroupsDrawn(progress.areGroupsDrawn());
        response.setKnockoutTeamCount(expectedKnockoutTeams(rules, teams.size()));
        response.setTeamsAdvancingPerTable(teamsAdvancingPerTable(rules));
        response.setChampionTeamId(progress.champion().orElse(null));
        response.setNextStep(toNextStep(progress.nextStep()));
        response.setPhases(progress.phases().stream().map(this::toPhase).toList());
        response.setTables(progress.tables().stream().map(table -> toTable(table, teamNames)).toList());
        response.setRounds(progress.rounds().stream().map(this::toRound).toList());
        return response;
    }

    private ChampionshipResponse baseResponse(EventsEntity event, List<TeamRef> teams, boolean scheduleConfigured) {
        ChampionshipResponse response = new ChampionshipResponse();
        response.setEventId(event.getId());
        EventFormatsEntity format = event.getEventFormat();
        if (format != null) {
            response.setFormatId(format.getId());
            response.setFormatName(format.getName());
        }
        response.setScheduleConfigured(scheduleConfigured);
        response.setTeams(teams.stream().map(this::toTeam).toList());
        return response;
    }

    /** How many teams will reach the knockout rounds, so the bracket can show all its rounds before they are played. */
    private Integer expectedKnockoutTeams(ChampionshipRules rules, int registeredTeams) {
        if (!rules.structure().phases().contains(ChampionshipPhase.KNOCKOUT)) {
            return null;
        }
        return switch (rules.structure()) {
            case KNOCKOUT -> registeredTeams;
            case GROUPS_KNOCKOUT -> rules.groupCount() * rules.teamsAdvancingPerGroup();
            default -> rules.playoffTeams();
        };
    }

    /** The teams that leave each points table for the knockout rounds (null when the format has none after its tables). */
    private Integer teamsAdvancingPerTable(ChampionshipRules rules) {
        return switch (rules.structure()) {
            case GROUPS_KNOCKOUT -> rules.teamsAdvancingPerGroup();
            case LEAGUE_KNOCKOUT, SWISS_KNOCKOUT -> rules.playoffTeams();
            default -> null;
        };
    }

    private ChampionshipResponse.NextStepResponse toNextStep(ChampionshipProgress.NextStep step) {
        ChampionshipResponse.NextStepResponse response = new ChampionshipResponse.NextStepResponse();
        response.setAction(step.action());
        response.setPhase(step.phase());
        response.setRoundNumber(step.roundNumber());
        response.setPendingMatches(step.pendingMatches());
        response.setRegisteredTeams(step.registeredTeams());
        response.setRequiredTeams(step.requiredTeams());
        response.setEvenTeamsRequired(step.evenTeamsRequired());
        response.setMatchesToResolve(step.matchesToResolve());
        return response;
    }

    private ChampionshipResponse.PhaseResponse toPhase(ChampionshipProgress.PhaseState state) {
        ChampionshipResponse.PhaseResponse response = new ChampionshipResponse.PhaseResponse();
        response.setPhase(state.phase());
        response.setStatus(state.status());
        return response;
    }

    private ChampionshipResponse.TeamResponse toTeam(TeamRef team) {
        ChampionshipResponse.TeamResponse response = new ChampionshipResponse.TeamResponse();
        response.setTeamId(team.id());
        response.setTeamName(team.name());
        response.setGroupNumber(team.groupNumber());
        response.setSeed(team.seed());
        return response;
    }

    /** Every line of the given tables, one table after the other (the group tables one after another). */
    public List<StandingEntryResponse> toStandingEntries(List<ChampionshipProgress.TableView> tables, Map<UUID, String> teamNames) {
        return tables.stream().flatMap(table -> toEntries(table, teamNames).stream()).toList();
    }

    private ChampionshipResponse.TableResponse toTable(ChampionshipProgress.TableView table, Map<UUID, String> teamNames) {
        ChampionshipResponse.TableResponse response = new ChampionshipResponse.TableResponse();
        response.setPhase(table.phase());
        response.setGroupNumber(table.groupNumber());
        response.setEntries(toEntries(table, teamNames));
        return response;
    }

    private List<StandingEntryResponse> toEntries(ChampionshipProgress.TableView table, Map<UUID, String> teamNames) {
        return table.rows().stream().map(row -> toEntry(row, teamNames, table.groupNumber())).toList();
    }

    private StandingEntryResponse toEntry(StandingRow row, Map<UUID, String> teamNames, Integer groupNumber) {
        StandingEntryResponse entry = new StandingEntryResponse();
        entry.setPosition(row.position());
        entry.setTeamId(row.teamId());
        entry.setTeamName(teamNames.get(row.teamId()));
        entry.setPlayed(row.played());
        entry.setWon(row.won());
        entry.setDrawn(row.drawn());
        entry.setLost(row.lost());
        entry.setGoalsFor(row.goalsFor());
        entry.setGoalsAgainst(row.goalsAgainst());
        entry.setGoalDifference(row.goalDifference());
        entry.setPoints(row.points());
        entry.setOpponentsPoints(row.opponentsPoints());
        entry.setGroupNumber(groupNumber);
        return entry;
    }

    private ChampionshipResponse.RoundResponse toRound(ChampionshipProgress.RoundView round) {
        ChampionshipResponse.RoundResponse response = new ChampionshipResponse.RoundResponse();
        response.setPhase(round.phase());
        response.setRoundNumber(round.roundNumber());
        response.setGroupNumber(round.groupNumber());
        response.setTies(round.ties().stream().map(this::toTie).toList());
        return response;
    }

    private ChampionshipResponse.TieResponse toTie(ChampionshipProgress.TieView tie) {
        ChampionshipResponse.TieResponse response = new ChampionshipResponse.TieResponse();
        response.setTieNumber(tie.tieNumber());
        response.setStage(tie.stage());
        response.setTeamAId(tie.teamAId());
        response.setTeamBId(tie.teamBId());
        response.setBye(tie.bye());
        response.setLegs(tie.legs().stream().map(this::toLeg).toList());
        response.setTeamAGoals(tie.teamAGoals());
        response.setTeamBGoals(tie.teamBGoals());
        response.setWinnerTeamId(tie.winnerTeamId());
        response.setNeedsTiebreak(tie.needsTiebreak());
        return response;
    }

    private ChampionshipResponse.LegResponse toLeg(MatchRef match) {
        ChampionshipResponse.LegResponse response = new ChampionshipResponse.LegResponse();
        response.setMatchId(match.id());
        response.setHomeTeamId(match.homeTeamId());
        response.setAwayTeamId(match.awayTeamId());
        response.setHomeScore(match.homeScore());
        response.setAwayScore(match.awayScore());
        response.setLegNumber(match.legNumber());
        response.setAdvancingTeamId(match.advancingTeamId());
        response.setMatchDate(match.matchDate());
        response.setStatus(match.status());
        return response;
    }
}
