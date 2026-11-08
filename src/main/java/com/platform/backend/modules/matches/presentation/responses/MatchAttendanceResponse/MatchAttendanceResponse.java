package com.platform.backend.modules.matches.presentation.responses.MatchAttendanceResponse;

import com.platform.backend.modules.matches.domain.enums.AttendanceStatus;
import com.platform.backend.modules.matches.presentation.responses.AttendeeResponse.AttendeeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Who will be at a match: the totals for anyone who sees it, and the names only for those it is about. */
@Data
@AllArgsConstructor
public class MatchAttendanceResponse {

    private UUID matchId;
    private int confirmedCount;
    private int declinedCount;
    /** The caller's own answer; null when they have not answered or their account is not a player. */
    private AttendanceStatus myStatus;
    /**
     * Who answered what: filled for an administrator and for the players of the two teams of the match, empty for
     * anyone else (a public event shows the totals, not the names of other organizations' players).
     */
    private List<AttendeeResponse> attendees;
}
