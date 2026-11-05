package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.filters.EventListFilter;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventRepository {

    EventsEntity save(EventsEntity event);

    Optional<EventsEntity> findById(UUID id);

    List<EventsEntity> findAllActive();

    /** One page of the active events the caller sees that match the filter, the ones that matter most first. */
    PageResult<EventsEntity> findActivePage(EventListFilter filter, PageQuery pageQuery);

    /** The active events with those ids among the ones the caller sees. */
    List<EventsEntity> findAllActiveByIds(Collection<UUID> ids);

    Optional<EventsEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    List<EventsEntity> findAllActiveByEventTypeId(UUID eventTypeId);

    List<EventsEntity> findAllActiveByEventTypeIdAndCreatedBy(UUID eventTypeId, UUID createdBy);

    boolean existsActiveByEventFormatId(UUID eventFormatId);

    boolean existsActiveByOrganizationId(UUID organizationId);

    void delete(EventsEntity event);

    void hardDelete(EventsEntity event);
}
