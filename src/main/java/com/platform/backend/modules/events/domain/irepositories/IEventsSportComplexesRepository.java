package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.EventsSportComplexesEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IEventsSportComplexesRepository {

    EventsSportComplexesEntity save(EventsSportComplexesEntity link);

    /** The active links of one event, in the order they were added. */
    List<EventsSportComplexesEntity> findAllActiveByEventId(UUID eventId);

    /** The active links of several events at once, so a list of events needs one query and not one per event. */
    List<EventsSportComplexesEntity> findAllActiveByEventIds(Collection<UUID> eventIds);

    /** Soft delete: the link is stamped as deleted by the caller and saved. */
    void delete(EventsSportComplexesEntity link);
}
