package com.platform.backend.modules.organizations.infrastructure.persistence.repositories;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.organizations.infrastructure.persistence.jpa.OrganizationsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrganizationRepository implements IOrganizationRepository {

    private final OrganizationsJpaRepository organizationsJpaRepository;

    @Override
    public OrganizationsEntity save(OrganizationsEntity organization) {
        return organizationsJpaRepository.save(organization);
    }

    @Override
    public List<OrganizationsEntity> findAllActive() {
        return organizationsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<OrganizationsEntity> findActiveById(UUID id) {
        return organizationsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return organizationsJpaRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name);
    }

    @Override
    public boolean existsByNameExcludingId(String name, UUID id) {
        return organizationsJpaRepository.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(name, id);
    }

    @Override
    public void delete(OrganizationsEntity organization) {
        organizationsJpaRepository.save(organization);
    }
}
