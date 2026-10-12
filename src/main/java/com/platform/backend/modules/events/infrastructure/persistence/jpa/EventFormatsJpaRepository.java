package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventFormatsJpaRepository extends JpaRepository<EventFormatsEntity, UUID> {

    List<EventFormatsEntity> findAllByDeletedAtIsNull();

    Optional<EventFormatsEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Case-insensitive: "Liga" and "liga" would be indistinguishable in the format selector.
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);
}
