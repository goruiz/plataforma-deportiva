package com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse;

import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMatchesResponse {

    private List<MatchResponse> matches;

    /** Null when all matches fit within the event end date. */
    private String warning;
}
