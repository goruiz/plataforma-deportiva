package com.platform.backend.modules.menu.infrastructure.persistence.repositories;

import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.menu.infrastructure.persistence.jpa.RolesMenuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RolesMenuRepository implements IRolesMenuRepository {

    private final RolesMenuJpaRepository rolesMenuJpaRepository;

    @Override
    public RolesMenuEntity save(RolesMenuEntity rolesMenu) {
        return rolesMenuJpaRepository.save(rolesMenu);
    }

    @Override
    public Optional<RolesMenuEntity> findById(UUID id) {
        return rolesMenuJpaRepository.findById(id);
    }

    @Override
    public List<RolesMenuEntity> findAllActive() {
        return rolesMenuJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<RolesMenuEntity> findActiveById(UUID id) {
        return rolesMenuJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(RolesMenuEntity rolesMenu) {
        rolesMenuJpaRepository.save(rolesMenu);
    }

    @Override
    public void hardDelete(RolesMenuEntity rolesMenu) {
        rolesMenuJpaRepository.delete(rolesMenu);
    }
}
