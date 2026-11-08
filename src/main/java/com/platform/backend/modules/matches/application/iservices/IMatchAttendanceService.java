package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.UpdateAttendanceRequest.UpdateAttendanceRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchAttendanceResponse.MatchAttendanceResponse;

import java.util.UUID;

/** Whether the players will be at a match: each one answers for themselves, only before the match starts. */
public interface IMatchAttendanceService {

    /** The totals of a match the caller sees, with the names when the caller is an administrator or plays in it. */
    MatchAttendanceResponse getByMatchId(UUID matchId);

    /**
     * The signed-in player says they will (or will not) be at the match, replacing an earlier answer. Only a player of
     * one of the two teams, and only while the match is still due to be played.
     */
    MatchAttendanceResponse respond(UUID matchId, UpdateAttendanceRequest request);

    /** The signed-in player takes their answer back. Safe to repeat. */
    MatchAttendanceResponse withdraw(UUID matchId);
}
