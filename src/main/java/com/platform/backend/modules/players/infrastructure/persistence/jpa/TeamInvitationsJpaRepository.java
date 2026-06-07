package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamInvitationsJpaRepository extends JpaRepository<TeamInvitationsEntity, UUID> {

    Optional<TeamInvitationsEntity> findByToken(String token);
}
