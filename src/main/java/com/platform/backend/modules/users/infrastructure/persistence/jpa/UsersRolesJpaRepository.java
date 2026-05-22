package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRolesJpaRepository extends JpaRepository<UsersRolesEntity, UUID> {

    List<UsersRolesEntity> findAllByDeletedAtIsNull();

    Optional<UsersRolesEntity> findByIdAndDeletedAtIsNull(UUID id);
}
