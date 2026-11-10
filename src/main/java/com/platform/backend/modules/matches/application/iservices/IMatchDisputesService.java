package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.domain.filters.DisputeFilter;
import com.platform.backend.modules.matches.presentation.requests.OpenDisputeRequest.OpenDisputeRequest;
import com.platform.backend.modules.matches.presentation.requests.ResolveDisputeRequest.ResolveDisputeRequest;
import com.platform.backend.modules.matches.presentation.responses.DisputeCountsResponse.DisputeCountsResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchDisputeResponse.MatchDisputeResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.List;
import java.util.UUID;

public interface IMatchDisputesService {

    /**
     * A team disputes the result of a match. An administrator may do it for any team at any time; otherwise it takes the
     * captain of the team and an open window (the organization's settings). The result stops being validated.
     */
    MatchDisputeResponse open(UUID matchId, OpenDisputeRequest request);

    /** One page of the disputes the caller sees (the organization's, or all for the platform owner), most recent first. */
    PageResult<MatchDisputeResponse> search(DisputeFilter filter, PageQuery pageQuery);

    /** How many disputes each state has for the filter's search (its own status is ignored). */
    DisputeCountsResponse countByStatus(DisputeFilter filter);

    /** The disputes against a match: all of them for an administrator, those of the caller's teams for anyone else. */
    List<MatchDisputeResponse> getByMatch(UUID matchId);

    /** The disputes of the teams the caller plays for. */
    List<MatchDisputeResponse> getMine();

    /**
     * Decides an open dispute. Accepting may correct the result, which goes through the same rules as recording one;
     * rejecting needs a note. Only an administrator or whoever created the event may do it.
     */
    MatchDisputeResponse resolve(UUID id, ResolveDisputeRequest request);
}
