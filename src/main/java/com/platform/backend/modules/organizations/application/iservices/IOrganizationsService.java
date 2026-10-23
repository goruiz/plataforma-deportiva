package com.platform.backend.modules.organizations.application.iservices;

import com.platform.backend.modules.organizations.presentation.requests.CreateOrganizationRequest.CreateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.requests.UpdateOrganizationRequest.UpdateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.responses.OrganizationResponse.OrganizationResponse;

import java.util.List;
import java.util.UUID;

public interface IOrganizationsService {

    List<OrganizationResponse> getAll();

    OrganizationResponse getById(UUID id);

    OrganizationResponse create(CreateOrganizationRequest request);

    OrganizationResponse update(UUID id, UpdateOrganizationRequest request);

    /** Soft delete; refused while the organization still has active users, events or teams. */
    void delete(UUID id);
}
