package com.platform.backend.modules.organizations.application.services;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.organizations.application.iservices.IOrganizationsService;
import com.platform.backend.modules.organizations.application.mappers.OrganizationMapper;
import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.enums.OrganizationStatus;
import com.platform.backend.modules.organizations.domain.exception.OrganizationError;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.organizations.presentation.requests.CreateOrganizationRequest.CreateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.requests.UpdateOrganizationRequest.UpdateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.responses.OrganizationResponse.OrganizationResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationsService implements IOrganizationsService {

    private static final String NAME_FIELD = "name";
    private static final String STATUS_FIELD = "status";

    private final IOrganizationRepository organizationRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;
    private final ITeamRepository teamRepository;
    private final OrganizationMapper organizationMapper;

    @Transactional(readOnly = true)
    @Override
    public List<OrganizationResponse> getAll() {
        return organizationRepository.findAllActive()
                .stream()
                .map(organizationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public OrganizationResponse getById(UUID id) {
        return organizationMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public OrganizationResponse create(CreateOrganizationRequest request) {
        String name = request.getName().trim();
        if (organizationRepository.existsByName(name)) {
            throw nameAlreadyExists();
        }

        OrganizationsEntity organization = organizationMapper.toEntity(request);
        organization.setName(name);
        organization.setDescription(blankToNull(request.getDescription()));
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Transactional
    @Override
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request) {
        OrganizationsEntity organization = findActiveOrThrow(id);
        validateNameIsFree(request, id);
        validateStatus(request.getStatus());

        organizationMapper.updateEntity(organization, request);
        if (request.getDescription() != null) {
            organization.setDescription(blankToNull(request.getDescription()));
        }
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        OrganizationsEntity organization = findActiveOrThrow(id);
        if (isInUse(id)) {
            throw new IllegalStateException(OrganizationError.IN_USE.name());
        }
        SoftDeleteHelper.prepareForDeletion(organization);
        organizationRepository.delete(organization);
    }

    private OrganizationsEntity findActiveOrThrow(UUID id) {
        return organizationRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));
    }

    /** The name is optional in an update; when sent it is trimmed in place so the mapper copies the clean value. */
    private void validateNameIsFree(UpdateOrganizationRequest request, UUID organizationId) {
        if (request.getName() == null) {
            return;
        }
        String name = request.getName().trim();
        if (organizationRepository.existsByNameExcludingId(name, organizationId)) {
            throw nameAlreadyExists();
        }
        request.setName(name);
    }

    private void validateStatus(String status) {
        if (status != null && !OrganizationStatus.isValid(status)) {
            throw new FieldValidationException(STATUS_FIELD, OrganizationError.INVALID_STATUS.name());
        }
    }

    // Users, events and teams point at their organization with a plain column, so nothing in the database stops a delete that orphans them.
    private boolean isInUse(UUID organizationId) {
        return userRepository.existsActiveByOrganizationId(organizationId)
                || eventRepository.existsActiveByOrganizationId(organizationId)
                || teamRepository.existsActiveByOrganizationId(organizationId);
    }

    private FieldValidationException nameAlreadyExists() {
        return new FieldValidationException(NAME_FIELD, OrganizationError.NAME_ALREADY_EXISTS.name());
    }

    /** A blank description means "no description": clients never have to deal with an empty string. */
    private String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
