package com.platform.backend.modules.matches.presentation.requests.ResolveDisputeRequest;

import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResolveDisputeRequest {

    /** True upholds the complaint; false keeps the result as it was. */
    @NotNull
    private Boolean accepted;

    /** Explains the decision. Rejecting needs it; the service refuses a blank one with a code. */
    @Size(max = MatchDisputesEntity.NOTE_MAX_LENGTH)
    private String note;

    // The corrected result, when accepting with one. Both goals or none; they go through the same rules as recording a
    // result in the match, so a level score may need a penalty shoot-out.
    private Integer homeScore;

    private Integer awayScore;

    private Integer homePenaltyScore;

    private Integer awayPenaltyScore;
}
