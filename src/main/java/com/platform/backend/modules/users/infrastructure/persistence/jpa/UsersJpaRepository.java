package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsersJpaRepository
        extends JpaRepository<UsersEntity, UUID> {

}