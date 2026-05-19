package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersJpaRepository
        extends JpaRepository<UsersEntity, UUID> {

    Optional<UsersEntity> findByEmail(String email);

    Optional<UsersEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    List<UsersEntity> findAllByDeletedAtIsNull();

    Optional<UsersEntity> findByIdAndDeletedAtIsNull(UUID id);
}