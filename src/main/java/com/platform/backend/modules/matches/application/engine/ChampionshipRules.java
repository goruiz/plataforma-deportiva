package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;

import java.util.List;

/**
 * The rules of a championship, read from the event's format. A rule the structure does not use is zero / false.
 */
public record ChampionshipRules(EventFormatStructure structure, boolean roundTrip,
                                int pointsForWin, int pointsForDraw, int pointsForLoss,
                                List<TiebreakerCriterion> tiebreakers, int swissRounds, int groupCount,
                                int teamsAdvancingPerGroup, int playoffTeams, boolean knockoutRoundTrip,
                                boolean thirdPlaceMatch, boolean grandFinalReset) {

    private static final int DEFAULT_POINTS_FOR_WIN = 3;
    private static final int DEFAULT_POINTS_FOR_DRAW = 1;
    private static final int DEFAULT_POINTS_FOR_LOSS = 0;
    private static final List<TiebreakerCriterion> DEFAULT_TIEBREAKERS =
            List.of(TiebreakerCriterion.GOAL_DIFFERENCE, TiebreakerCriterion.GOALS_FOR);

    public static ChampionshipRules of(EventFormatsEntity format) {
        List<TiebreakerCriterion> tiebreakers = TiebreakerCriterion.fromStored(format.getTiebreakers());
        return new ChampionshipRules(
                format.getStructure(),
                Boolean.TRUE.equals(format.getRoundTrip()),
                valueOrZero(format.getPointsForWin()),
                valueOrZero(format.getPointsForDraw()),
                valueOrZero(format.getPointsForLoss()),
                tiebreakers == null ? List.of() : tiebreakers,
                valueOrZero(format.getSwissRounds()),
                valueOrZero(format.getGroupCount()),
                valueOrZero(format.getTeamsAdvancingPerGroup()),
                valueOrZero(format.getPlayoffTeams()),
                Boolean.TRUE.equals(format.getKnockoutRoundTrip()),
                Boolean.TRUE.equals(format.getThirdPlaceMatch()),
                Boolean.TRUE.equals(format.getGrandFinalReset()));
    }

    /**
     * What an event without a format has always been: a one-leg league (3 points for a win, 1 for a draw), ordered by
     * goal difference and then goals scored. Events created by the mobile app are like this.
     */
    public static ChampionshipRules legacyLeague() {
        return new ChampionshipRules(EventFormatStructure.ROUND_ROBIN, false,
                DEFAULT_POINTS_FOR_WIN, DEFAULT_POINTS_FOR_DRAW, DEFAULT_POINTS_FOR_LOSS, DEFAULT_TIEBREAKERS,
                0, 0, 0, 0, false, false, false);
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
