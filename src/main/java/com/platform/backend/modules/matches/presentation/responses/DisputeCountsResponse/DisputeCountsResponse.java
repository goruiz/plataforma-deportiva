package com.platform.backend.modules.matches.presentation.responses.DisputeCountsResponse;

import lombok.Data;

/** How many disputes each state has (for one search), so a screen can show the number on each filter. */
@Data
public class DisputeCountsResponse {

    private long all;
    private long open;
    private long accepted;
    private long rejected;
}
