package com.platform.backend.modules.teams.infrastructure.persistence.jpa;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamsJpaRepository extends JpaRepository<TeamsEntity, UUID> {

    List<TeamsEntity> findAllByDeletedAtIsNull();

    Optional<TeamsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);

    @Query("SELECT t FROM TeamsEntity t WHERE t.id IN :ids AND t.deletedAt IS NULL")
    List<TeamsEntity> findAllActiveByIds(@Param("ids") Collection<UUID> ids);



}

