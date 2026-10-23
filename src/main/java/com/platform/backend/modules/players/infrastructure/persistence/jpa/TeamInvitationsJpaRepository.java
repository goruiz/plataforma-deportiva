package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamInvitationsJpaRepository extends TenantScopedJpaRepository<TeamInvitationsEntity> {

    Optional<TeamInvitationsEntity> findByToken(String token);
}
