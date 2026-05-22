package com.platform.backend.modules.courts.application.iservices;

import com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest.CreateCourtRequest;
import com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest.UpdateCourtRequest;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;

import java.util.List;
import java.util.UUID;

public interface ICourtsService {

    List<CourtResponse> getAll();

    CourtResponse getById(UUID id);

    CourtResponse create(CreateCourtRequest request);

    CourtResponse update(UUID id, UpdateCourtRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
