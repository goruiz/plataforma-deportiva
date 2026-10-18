package com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest;

import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateEventFormatRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    private EventFormatStructure structure;

    // Which of the rules below are required depends on the structure (EventFormatRulesService checks it);
    // the ones the structure does not use are ignored.

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
