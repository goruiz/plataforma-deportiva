package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchTimingService;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
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
 * <p>
 * Two of the match rules of the organization's settings lengthen that slot: the {@code matchWaitMinutes} every match
 * waits for its teams (so it may start that late) and the {@code extraTimeMinutes} an elimination match that must
 * produce a winner may need. Both are part of when the match ends, so the court stays reserved, the calendar leaves the
 * room, and a result can only be recorded once everything the match may need is over.
 */
@Service
public class MatchTimingService implements IMatchTimingService {

    private final IEventScheduleConfigRepository configRepository;
    private final MatchSettingsReader settingsReader;
    private final int defaultSlotMinutes;

    public MatchTimingService(IEventScheduleConfigRepository configRepository,
                              MatchSettingsReader settingsReader,
                              @Value("${app.matches.default-duration-minutes:60}") int defaultDurationMinutes,
                              @Value("${app.matches.default-break-minutes:15}") int defaultBreakMinutes) {
        this.configRepository = configRepository;
        this.settingsReader = settingsReader;
        this.defaultSlotMinutes = defaultDurationMinutes + defaultBreakMinutes;
    }

    /** What an event adds to the kick-off to know when its matches end: read once per event, however many matches. */
    private record EventTiming(int slotMinutes, int waitMinutes, int extraTimeMinutes) {

        int minutesOf(MatchesEntity match) {
            return slotMinutes + waitMinutes + (needsExtraTime(match) ? extraTimeMinutes : 0);
        }
    }

    @Override
    public Map<UUID, LocalDateTime> getEndTimes(Collection<MatchesEntity> matches) {
        Map<UUID, EventTiming> timingByEvent = new HashMap<>();
        Map<UUID, LocalDateTime> endTimes = new HashMap<>();

        for (MatchesEntity match : matches) {
            EventTiming timing = timingByEvent.computeIfAbsent(match.getEventId(), this::timingOf);
            endTimes.put(match.getId(), match.getMatchDate().plusMinutes(timing.minutesOf(match)));
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

    private EventTiming timingOf(UUID eventId) {
        SettingsValues settings = settingsReader.forEvent(eventId);
        return new EventTiming(slotMinutesOf(eventId), settings.matchWaitMinutes(), settings.extraTimeMinutes());
    }

    private int slotMinutesOf(UUID eventId) {
        if (eventId == null) {
            return defaultSlotMinutes;
        }
        return configRepository.findActiveByEventId(eventId)
                .map(config -> config.getMatchDurationMinutes() + config.getBreakBetweenMatchesMinutes())
                .orElse(defaultSlotMinutes);
    }

    /**
     * Only a match of an elimination that decides a tie can go to extra time: a league match ends level, and the first
     * leg of a two-legged tie too. A single match (no leg number) and the second leg decide it.
     */
    static boolean needsExtraTime(MatchesEntity match) {
        MatchStage stage = match.getStage();
        boolean decidesATie = match.getLegNumber() == null || match.getLegNumber() >= 2;
        return stage != null && stage.phase().isElimination() && decidesATie;
    }
}
