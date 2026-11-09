package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.EventsSportComplexesEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventsSportComplexesRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventsSportComplexesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventsSportComplexesRepository implements IEventsSportComplexesRepository {

    private final EventsSportComplexesJpaRepository jpaRepository;

    @Override
    public EventsSportComplexesEntity save(EventsSportComplexesEntity link) {
        return jpaRepository.save(link);
    }

    @Override
    public List<EventsSportComplexesEntity> findAllActiveByEventId(UUID eventId) {
        return jpaRepository.findAllByEventIdAndDeletedAtIsNullOrderByCreatedAtAsc(eventId);
    }

    @Override
    public List<EventsSportComplexesEntity> findAllActiveByEventIds(Collection<UUID> eventIds) {
        return eventIds.isEmpty() ? List.of() : jpaRepository.findAllByEventIdInAndDeletedAtIsNullOrderByCreatedAtAsc(eventIds);
    }

    @Override
    public void delete(EventsSportComplexesEntity link) {
        jpaRepository.save(link);
    }
}
