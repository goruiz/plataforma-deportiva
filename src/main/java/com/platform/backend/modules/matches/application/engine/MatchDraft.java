package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.util.UUID;

/**
 * A match the championship wants to create: who plays whom and where it sits in the competition. It still has no
 * date; the scheduler gives it one from the event's calendar configuration.
 *
 * @param roundNumber a league or group matchday, a Swiss round, or a round of the knockout rounds
 * @param groupNumber only in the group stage
 * @param tieNumber   only in eliminations: which tie of the round (a tie has one match, or two when played home and away)
 * @param legNumber   1 or 2 in a tie played home and away, otherwise null
 */
public record MatchDraft(UUID homeTeamId, UUID awayTeamId, MatchStage stage, Integer roundNumber,
                         Integer groupNumber, Integer tieNumber, Integer legNumber) {

    public static MatchDraft of(Pairing pairing, MatchStage stage, Integer roundNumber) {
        return new MatchDraft(pairing.homeTeamId(), pairing.awayTeamId(), stage, roundNumber, null, null, null);
    }

    public MatchDraft inGroup(int groupNumber) {
        return new MatchDraft(homeTeamId, awayTeamId, stage, roundNumber, groupNumber, tieNumber, legNumber);
    }
}
