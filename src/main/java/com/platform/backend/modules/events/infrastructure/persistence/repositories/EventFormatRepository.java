package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventFormatsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventFormatRepository implements IEventFormatRepository {

    private final EventFormatsJpaRepository eventFormatsJpaRepository;

    @Override
    public EventFormatsEntity save(EventFormatsEntity eventFormat) {
        return eventFormatsJpaRepository.save(eventFormat);
    }

    @Override
    public List<EventFormatsEntity> findAllActive() {
        return eventFormatsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<EventFormatsEntity> findActiveById(UUID id) {
        return eventFormatsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return eventFormatsJpaRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name);
    }

    @Override
    public boolean existsByNameExcludingId(String name, UUID id) {
        return eventFormatsJpaRepository.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(name, id);
    }

    @Override
    public void delete(EventFormatsEntity eventFormat) {
        eventFormatsJpaRepository.save(eventFormat);
    }
}
