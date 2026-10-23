package com.platform.backend.modules.organizations.infrastructure.persistence.jpa;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationsJpaRepository extends JpaRepository<OrganizationsEntity, UUID> {

    List<OrganizationsEntity> findAllByDeletedAtIsNull();

    Optional<OrganizationsEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Liga Norte" and "liga norte" would be indistinguishable in every organization list.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}
