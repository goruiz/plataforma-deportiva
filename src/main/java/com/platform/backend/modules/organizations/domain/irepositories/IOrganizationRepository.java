package com.platform.backend.modules.organizations.domain.irepositories;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IOrganizationRepository {

    OrganizationsEntity save(OrganizationsEntity organization);

    List<OrganizationsEntity> findAllActive();

    Optional<OrganizationsEntity> findActiveById(UUID id);

    /** Names are compared ignoring case. */
    boolean existsByName(String name);

    /** True when an active organization other than {@code id} already uses {@code name} (ignoring case). */
    boolean existsByNameExcludingId(String name, UUID id);

    void delete(OrganizationsEntity organization);
}
