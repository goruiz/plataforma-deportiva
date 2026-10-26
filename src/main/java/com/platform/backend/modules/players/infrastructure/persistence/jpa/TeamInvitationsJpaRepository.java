package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationsJpaRepository extends TenantScopedJpaRepository<TeamInvitationsEntity> {

    Optional<TeamInvitationsEntity> findByToken(String token);

    List<TeamInvitationsEntity> findAllByEmailIgnoreCaseAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            String email, String status);
}
