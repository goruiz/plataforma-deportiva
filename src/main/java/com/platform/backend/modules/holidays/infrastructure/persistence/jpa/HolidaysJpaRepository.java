package com.platform.backend.modules.holidays.infrastructure.persistence.jpa;

import com.platform.backend.modules.holidays.domain.entities.HolidaysEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HolidaysJpaRepository extends JpaRepository<HolidaysEntity, UUID> {

    List<HolidaysEntity> findAllByDeletedAtIsNull();

    Optional<HolidaysEntity> findByIdAndDeletedAtIsNull(UUID id);
}
