package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.ChampionshipResponse.ChampionshipResponse;

import java.util.UUID;

/**
 * Runs an event's championship from its format: the phases, the tables and the rounds. Every operation answers with
 * the championship as it stands afterwards, so a client only has to show the answer.
 */
public interface IChampionshipService {

    ChampionshipResponse getChampionship(UUID eventId);

    /**
     * Draws the teams into the groups of the format (a new draw replaces the previous one).
     *
     * @throws IllegalStateException with a {@code ChampionshipError} or {@code ChampionshipAction} code when it cannot be done
     */
    ChampionshipResponse drawGroups(UUID eventId);

    /**
     * Does the next step of the championship: generates the whole calendar of the league or the group stage, or the
     * next round of the Swiss, knockout or double-elimination phase, and schedules its matches.
     *
     * @throws IllegalStateException {@code SCHEDULE_CONFIG_REQUIRED}, {@code WAITING_RESULTS}, {@code NEEDS_TIEBREAK},
     *         {@code NOT_ENOUGH_TEAMS}, {@code FINISHED} or {@code NO_FORMAT} when there is nothing to generate now
     */
    ChampionshipResponse generateNext(UUID eventId);

    /** Says which team advances from a tie that ended level on goals. */
    ChampionshipResponse setAdvancingTeam(UUID eventId, UUID matchId, UUID teamId);
}
