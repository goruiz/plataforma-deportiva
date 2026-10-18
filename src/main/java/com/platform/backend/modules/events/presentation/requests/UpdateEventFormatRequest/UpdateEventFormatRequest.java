package com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest;

import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateEventFormatRequest {

    // Null means "leave unchanged"; a value, when sent, can't be blank.
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    // Null also leaves each rule unchanged. The result (stored rules + the ones sent) must be a complete set
    // for its structure, and rules the structure does not use are cleared (EventFormatRulesService).

    private EventFormatStructure structure;

    private Boolean roundTrip;

    @Min(0) @Max(10)
    private Integer pointsForWin;

    @Min(0) @Max(10)
    private Integer pointsForDraw;

    @Min(0) @Max(10)
    private Integer pointsForLoss;

    /** In priority order; an empty list means ties are not broken. */
    private List<TiebreakerCriterion> tiebreakers;

    @Min(2) @Max(20)
    private Integer swissRounds;

    @Min(2) @Max(16)
    private Integer groupCount;

    @Min(1) @Max(8)
    private Integer teamsAdvancingPerGroup;

    @Min(2) @Max(64)
    private Integer playoffTeams;

    private Boolean knockoutRoundTrip;

    private Boolean thirdPlaceMatch;

    private Boolean grandFinalReset;
}
