package com.platform.backend.modules.matches.presentation.responses.ChampionshipResponse;

import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.enums.PhaseStatus;
import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Where an event's championship stands: the phases and which one is running, the points tables, every round with its
 * ties, the champion and what the administrator can do next. Everything is derived from the event's format and its
 * matches, so it is always consistent with them.
 */
@Data
public class ChampionshipResponse {

    private UUID eventId;
    private UUID formatId;
    private String formatName;
    /** Null when the event has no format, or its format has no rules yet: there is no championship to run. */
    private EventFormatStructure structure;
    /** True once the calendar configuration (play days, times, durations) was saved: matches cannot be generated before. */
    private boolean scheduleConfigured;
    private boolean groupsDrawn;
    /** How many teams reach the knockout rounds (null when the format has none): the bracket shows its rounds from this. */
    private Integer knockoutTeamCount;
    private UUID championTeamId;
    private NextStepResponse nextStep;
    private List<PhaseResponse> phases;
    private List<TeamResponse> teams;
    private List<TableResponse> tables;
    private List<RoundResponse> rounds;

    @Data
    public static class PhaseResponse {
        private ChampionshipPhase phase;
        private PhaseStatus status;
    }

    @Data
    public static class NextStepResponse {
        private ChampionshipAction action;
        private ChampionshipPhase phase;
        /** The round that would be generated (Swiss rounds and eliminations). */
        private Integer roundNumber;
        private int pendingMatches;
        private int registeredTeams;
        private int requiredTeams;
        private boolean evenTeamsRequired;
        /** The matches whose tie ended level: an administrator has to say which team advances in each. */
        private List<UUID> matchesToResolve;
    }

    @Data
    public static class TeamResponse {
        private UUID teamId;
        private String teamName;
        private Integer groupNumber;
        private Integer seed;
    }

    @Data
    public static class TableResponse {
        private ChampionshipPhase phase;
        /** Only in the group stage: each group has its own table. */
        private Integer groupNumber;
        private List<StandingEntryResponse> entries;
    }

    @Data
    public static class RoundResponse {
        private ChampionshipPhase phase;
        private int roundNumber;
        private Integer groupNumber;
        private List<TieResponse> ties;
    }

    /** A match, or the two legs of a tie, between two teams. In a league or Swiss round every match is a tie of its own. */
    @Data
    public static class TieResponse {
        private int tieNumber;
        private MatchStage stage;
        private UUID teamAId;
        /** Null in the first round of a knockout when {@link #bye} is true: the team advances without playing. */
        private UUID teamBId;
        private boolean bye;
        private List<LegResponse> legs;
        private int teamAGoals;
        private int teamBGoals;
        /** Only in eliminations, once the tie is decided. */
        private UUID winnerTeamId;
        private boolean needsTiebreak;
    }

    @Data
    public static class LegResponse {
        private UUID matchId;
        private UUID homeTeamId;
        private UUID awayTeamId;
        private Integer homeScore;
        private Integer awayScore;
        private Integer legNumber;
        private UUID advancingTeamId;
        private LocalDateTime matchDate;
        private String status;
    }
}
