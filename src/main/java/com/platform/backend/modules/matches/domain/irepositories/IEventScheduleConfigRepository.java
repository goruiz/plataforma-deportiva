package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;

import java.util.Optional;
import java.util.UUID;

public interface IEventScheduleConfigRepository {

    EventScheduleConfigEntity save(EventScheduleConfigEntity config);

    Optional<EventScheduleConfigEntity> findActiveByEventId(UUID eventId);
}
