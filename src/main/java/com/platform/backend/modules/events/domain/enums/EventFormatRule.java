package com.platform.backend.modules.events.domain.enums;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A rule a format can have. Each {@link EventFormatStructure} declares which of them it uses; the others mean
 * nothing for it and are stored as null. The field name is the one requests, responses and validation errors use.
 */
public enum EventFormatRule {
    ROUND_TRIP("roundTrip", true, EventFormatsEntity::getRoundTrip, format -> format.setRoundTrip(null)),
    POINTS_FOR_WIN("pointsForWin", false, EventFormatsEntity::getPointsForWin, format -> format.setPointsForWin(null)),
    POINTS_FOR_DRAW("pointsForDraw", false, EventFormatsEntity::getPointsForDraw, format -> format.setPointsForDraw(null)),
    POINTS_FOR_LOSS("pointsForLoss", false, EventFormatsEntity::getPointsForLoss, format -> format.setPointsForLoss(null)),
    TIEBREAKERS("tiebreakers", false, EventFormatsEntity::getTiebreakers, format -> format.setTiebreakers(null)),
    SWISS_ROUNDS("swissRounds", false, EventFormatsEntity::getSwissRounds, format -> format.setSwissRounds(null)),
    GROUP_COUNT("groupCount", false, EventFormatsEntity::getGroupCount, format -> format.setGroupCount(null)),
    TEAMS_ADVANCING_PER_GROUP("teamsAdvancingPerGroup", false, EventFormatsEntity::getTeamsAdvancingPerGroup,
            format -> format.setTeamsAdvancingPerGroup(null)),
    PLAYOFF_TEAMS("playoffTeams", false, EventFormatsEntity::getPlayoffTeams, format -> format.setPlayoffTeams(null)),
    KNOCKOUT_ROUND_TRIP("knockoutRoundTrip", true, EventFormatsEntity::getKnockoutRoundTrip,
            format -> format.setKnockoutRoundTrip(null)),
    THIRD_PLACE_MATCH("thirdPlaceMatch", true, EventFormatsEntity::getThirdPlaceMatch,
            format -> format.setThirdPlaceMatch(null)),
    GRAND_FINAL_RESET("grandFinalReset", true, EventFormatsEntity::getGrandFinalReset,
            format -> format.setGrandFinalReset(null));

    private final String fieldName;
    private final boolean yesNo;
    private final Function<EventFormatsEntity, Object> reader;
    private final Consumer<EventFormatsEntity> clearer;

    EventFormatRule(String fieldName, boolean yesNo, Function<EventFormatsEntity, Object> reader,
                    Consumer<EventFormatsEntity> clearer) {
        this.fieldName = fieldName;
        this.yesNo = yesNo;
        this.reader = reader;
        this.clearer = clearer;
    }

    public String fieldName() { return fieldName; }

    /** A yes/no rule: the formats of a structure are every combination of these. */
    public boolean isYesNo() { return yesNo; }

    /** The rule's value in the format, null when it has none. */
    public Object valueIn(EventFormatsEntity format) { return reader.apply(format); }

    public void clearIn(EventFormatsEntity format) { clearer.accept(format); }
}
