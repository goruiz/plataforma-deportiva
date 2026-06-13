package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.EventScheduleConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventScheduleConfigRepository implements IEventScheduleConfigRepository {

    private final EventScheduleConfigJpaRepository jpaRepository;

    @Override
    public EventScheduleConfigEntity save(EventScheduleConfigEntity config) {
        return jpaRepository.save(config);
    }

    @Override
    public Optional<EventScheduleConfigEntity> findActiveByEventId(UUID eventId) {
        return jpaRepository.findByEventIdAndDeletedAtIsNull(eventId);
    }
}
