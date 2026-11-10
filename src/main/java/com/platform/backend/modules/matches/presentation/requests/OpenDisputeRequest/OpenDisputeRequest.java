package com.platform.backend.modules.matches.presentation.requests.OpenDisputeRequest;

import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class OpenDisputeRequest {

    /** The team that disputes: one of the two that played the match. */
    @NotNull
    private UUID teamId;

    /** Why the result is wrong; the service refuses it when blank, with a code the client can translate. */
    @Size(max = MatchDisputesEntity.REASON_MAX_LENGTH)
    private String reason;

    /** The result the team says is right; both goals or none. */
    private Integer proposedHomeScore;

    private Integer proposedAwayScore;
}
