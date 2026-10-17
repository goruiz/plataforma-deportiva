package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** When a match is over: its kick-off plus the time it takes to play it and the break after it. */
public interface IMatchTimingService {

    /** The end of each match, by match id. Each event's schedule config is read only once, however many matches it has. */
    Map<UUID, LocalDateTime> getEndTimes(Collection<MatchesEntity> matches);

    LocalDateTime getEndTime(MatchesEntity match);

    /** True from the very minute the match ends: a 10:00 match of 60 minutes with a 15-minute break is over at 11:15. */
    boolean hasEnded(MatchesEntity match, LocalDateTime now);
}
