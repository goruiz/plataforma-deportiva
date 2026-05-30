package com.platform.backend.modules.sportcomplexes.application.iservices;

import com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest.CreateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest.UpdateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;

import java.util.List;
import java.util.UUID;

public interface ISportComplexesService {

    List<SportComplexResponse> getAll();

    SportComplexResponse getById(UUID id);

    SportComplexResponse create(CreateSportComplexRequest request);

    SportComplexResponse update(UUID id, UpdateSportComplexRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
