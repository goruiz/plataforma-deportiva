package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.filters.EventListFilter;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventSpecifications;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventsJpaRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.infraestructure.pagination.SpringPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventRepository implements IEventRepository {

    private final EventsJpaRepository eventsJpaRepository;

    @Override
    public EventsEntity save(EventsEntity event) {
        return eventsJpaRepository.save(event);
    }

    @Override
    public Optional<EventsEntity> findById(UUID id) {
        return eventsJpaRepository.findById(id);
    }

    @Override
    public List<EventsEntity> findAllActive() {
        return eventsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public PageResult<EventsEntity> findActivePage(EventListFilter filter, PageQuery pageQuery) {
        // Unsorted on purpose: the specification orders by relevance, which a plain sort cannot say.
        Pageable pageable = SpringPagination.toPageable(pageQuery, Sort.unsorted());
        return SpringPagination.toResult(eventsJpaRepository.findAll(EventSpecifications.matching(filter), pageable), pageQuery);
    }

    @Override
    public List<EventsEntity> findAllActiveByIds(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : eventsJpaRepository.findAllActiveByIds(ids);
    }

    @Override
    public Optional<EventsEntity> findActiveById(UUID id) {
        return eventsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return eventsJpaRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public List<EventsEntity> findAllActiveByEventTypeId(UUID eventTypeId) {
        return eventsJpaRepository.findAllByEventTypeIdAndDeletedAtIsNull(eventTypeId);
    }

    @Override
    public List<EventsEntity> findAllActiveByEventTypeIdAndCreatedBy(UUID eventTypeId, UUID createdBy) {
        return eventsJpaRepository.findAllByEventTypeIdAndCreatedByAndDeletedAtIsNull(eventTypeId, createdBy);
    }

    @Override
    public boolean existsActiveByEventFormatId(UUID eventFormatId) {
        return eventsJpaRepository.existsByEventFormatIdAndDeletedAtIsNull(eventFormatId);
    }

    @Override
    public boolean existsActiveByOrganizationId(UUID organizationId) {
        return eventsJpaRepository.existsByOrganizationIdAndDeletedAtIsNull(organizationId);
    }

    @Override
    public void delete(EventsEntity event) {
        eventsJpaRepository.save(event);
    }

    @Override
    public void hardDelete(EventsEntity event) {
        eventsJpaRepository.delete(event);
    }
}
