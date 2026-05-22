package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.CreateMatchDetailRequest.CreateMatchDetailRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchDetailRequest.UpdateMatchDetailRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchDetailResponse.MatchDetailResponse;

import java.util.List;
import java.util.UUID;

public interface IMatchDetailsService {

    List<MatchDetailResponse> getAll();

    MatchDetailResponse getById(UUID id);

    MatchDetailResponse create(CreateMatchDetailRequest request);

    MatchDetailResponse update(UUID id, UpdateMatchDetailRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
