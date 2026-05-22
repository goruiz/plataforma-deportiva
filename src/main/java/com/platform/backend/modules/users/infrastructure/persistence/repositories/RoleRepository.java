package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.RolesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleRepository implements IRoleRepository {

    private final RolesJpaRepository rolesJpaRepository;

    @Override
    public RolesEntity save(RolesEntity role) {
        return rolesJpaRepository.save(role);
    }

    @Override
    public Optional<RolesEntity> findById(UUID id) {
        return rolesJpaRepository.findById(id);
    }

    @Override
    public List<RolesEntity> findAllActive() {
        return rolesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<RolesEntity> findActiveById(UUID id) {
        return rolesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return rolesJpaRepository.existsByName(name);
    }

    @Override
    public void delete(RolesEntity role) {
        rolesJpaRepository.save(role);
    }

    @Override
    public void hardDelete(RolesEntity role) {
        rolesJpaRepository.delete(role);
    }
}
