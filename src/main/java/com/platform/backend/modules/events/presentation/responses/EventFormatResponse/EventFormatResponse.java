package com.platform.backend.modules.events.presentation.responses.EventFormatResponse;

import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EventFormatResponse {

    private UUID id;
    private String name;
    private String description;

    // Null in the predefined formats: they belong to nobody, every organization uses them and only the platform owner changes them.
    private UUID organizationId;

    // Null in formats created before rules existed, and in every rule the structure does not use.
    private EventFormatStructure structure;
    private Boolean roundTrip;
    private Integer pointsForWin;
    private Integer pointsForDraw;
    private Integer pointsForLoss;
    private List<TiebreakerCriterion> tiebreakers;
    private Integer swissRounds;
    private Integer groupCount;
    private Integer teamsAdvancingPerGroup;
    private Integer playoffTeams;
    private Boolean knockoutRoundTrip;
    private Boolean thirdPlaceMatch;
    private Boolean grandFinalReset;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
