package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchTimingService;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A match takes the duration and the break between matches of its event's schedule config, the same slot the
 * calendar generator reserves for it. A match of an event without a config (created by hand) takes the defaults.
 */
@Service
public class MatchTimingService implements IMatchTimingService {

    private final IEventScheduleConfigRepository configRepository;
    private final int defaultSlotMinutes;

    public MatchTimingService(IEventScheduleConfigRepository configRepository,
                              @Value("${app.matches.default-duration-minutes:60}") int defaultDurationMinutes,
                              @Value("${app.matches.default-break-minutes:15}") int defaultBreakMinutes) {
        this.configRepository = configRepository;
        this.defaultSlotMinutes = defaultDurationMinutes + defaultBreakMinutes;
    }

    @Override
    public Map<UUID, LocalDateTime> getEndTimes(Collection<MatchesEntity> matches) {
        Map<UUID, Integer> slotMinutesByEvent = new HashMap<>();
        Map<UUID, LocalDateTime> endTimes = new HashMap<>();

        for (MatchesEntity match : matches) {
            int slotMinutes = slotMinutesByEvent.computeIfAbsent(match.getEventId(), this::slotMinutesOf);
            endTimes.put(match.getId(), match.getMatchDate().plusMinutes(slotMinutes));
        }
        return endTimes;
    }

    @Override
    public LocalDateTime getEndTime(MatchesEntity match) {
        return getEndTimes(List.of(match)).get(match.getId());
    }

    @Override
    public boolean hasEnded(MatchesEntity match, LocalDateTime now) {
        return !getEndTime(match).isAfter(now);
    }

    private int slotMinutesOf(UUID eventId) {
        if (eventId == null) {
            return defaultSlotMinutes;
        }
        return configRepository.findActiveByEventId(eventId)
                .map(config -> config.getMatchDurationMinutes() + config.getBreakBetweenMatchesMinutes())
                .orElse(defaultSlotMinutes);
    }
}
