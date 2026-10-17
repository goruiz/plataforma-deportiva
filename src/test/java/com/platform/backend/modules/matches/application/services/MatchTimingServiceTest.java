package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchTimingServiceTest {

    private static final int DEFAULT_DURATION = 90;
    private static final int DEFAULT_BREAK = 10;
    private static final LocalDateTime TEN_AM = LocalDateTime.of(2026, 10, 17, 10, 0);

    @Mock private IEventScheduleConfigRepository configRepository;

    private MatchTimingService timingService;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        timingService = new MatchTimingService(configRepository, DEFAULT_DURATION, DEFAULT_BREAK);
        eventId = UUID.randomUUID();
    }

    private void eventConfig(int duration, int breakBetweenMatches) {
        EventScheduleConfigEntity config = new EventScheduleConfigEntity();
        config.setMatchDurationMinutes(duration);
        config.setBreakBetweenMatchesMinutes(breakBetweenMatches);
        when(configRepository.findActiveByEventId(eventId)).thenReturn(Optional.of(config));
    }

    private MatchesEntity matchAt(LocalDateTime kickOff, UUID forEvent) {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setMatchDate(kickOff);
        match.setEventId(forEvent);
        return match;
    }

    @Test
    void getEndTime_whenTheEventHasAConfig_thenKickOffPlusDurationPlusBreak() {
        eventConfig(60, 15);

        // 10:00 + 60 minutes of play + 15 of break = 11:15
        assertThat(timingService.getEndTime(matchAt(TEN_AM, eventId))).isEqualTo(LocalDateTime.of(2026, 10, 17, 11, 15));
    }

    @Test
    void getEndTime_whenTheEventHasNoConfig_thenTheConfiguredDefaultsApply() {
        when(configRepository.findActiveByEventId(eventId)).thenReturn(Optional.empty());

        assertThat(timingService.getEndTime(matchAt(TEN_AM, eventId))).isEqualTo(TEN_AM.plusMinutes(DEFAULT_DURATION + DEFAULT_BREAK));
    }

    @Test
    void getEndTime_whenTheMatchHasNoEvent_thenTheDefaultsApplyWithoutLookingForAConfig() {
        assertThat(timingService.getEndTime(matchAt(TEN_AM, null))).isEqualTo(TEN_AM.plusMinutes(DEFAULT_DURATION + DEFAULT_BREAK));
        verify(configRepository, never()).findActiveByEventId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getEndTime_whenTheMatchEndsAfterMidnight_thenItEndsTheNextDay() {
        eventConfig(120, 0);

        assertThat(timingService.getEndTime(matchAt(LocalDateTime.of(2026, 10, 17, 23, 0), eventId)))
                .isEqualTo(LocalDateTime.of(2026, 10, 18, 1, 0));
    }

    @Test
    void getEndTimes_whenSeveralMatchesShareAnEvent_thenItsConfigIsReadOnce() {
        eventConfig(60, 15);
        MatchesEntity first = matchAt(TEN_AM, eventId);
        MatchesEntity second = matchAt(TEN_AM.plusHours(2), eventId);

        Map<UUID, LocalDateTime> endTimes = timingService.getEndTimes(List.of(first, second));

        assertThat(endTimes).containsEntry(first.getId(), TEN_AM.plusMinutes(75)).containsEntry(second.getId(), TEN_AM.plusHours(2).plusMinutes(75));
        verify(configRepository, times(1)).findActiveByEventId(eventId);
    }

    @Test
    void getEndTimes_whenMatchesBelongToDifferentEvents_thenEachUsesItsOwnConfig() {
        UUID otherEventId = UUID.randomUUID();
        eventConfig(60, 15);
        EventScheduleConfigEntity otherConfig = new EventScheduleConfigEntity();
        otherConfig.setMatchDurationMinutes(40);
        otherConfig.setBreakBetweenMatchesMinutes(5);
        when(configRepository.findActiveByEventId(otherEventId)).thenReturn(Optional.of(otherConfig));
        MatchesEntity inFirstEvent = matchAt(TEN_AM, eventId);
        MatchesEntity inOtherEvent = matchAt(TEN_AM, otherEventId);

        Map<UUID, LocalDateTime> endTimes = timingService.getEndTimes(List.of(inFirstEvent, inOtherEvent));

        assertThat(endTimes.get(inFirstEvent.getId())).isEqualTo(TEN_AM.plusMinutes(75));
        assertThat(endTimes.get(inOtherEvent.getId())).isEqualTo(TEN_AM.plusMinutes(45));
    }

    // ─── hasEnded: the example of the requirement ───────────────────────────────

    @Test
    void hasEnded_whenAMatchOf60MinutesWith15OfBreakStartedAt10_thenNotBefore1115AndYesFrom1115() {
        eventConfig(60, 15);
        MatchesEntity match = matchAt(TEN_AM, eventId);

        assertThat(timingService.hasEnded(match, LocalDateTime.of(2026, 10, 17, 10, 0))).as("at kick-off").isFalse();
        assertThat(timingService.hasEnded(match, LocalDateTime.of(2026, 10, 17, 11, 0))).as("when the 60 minutes are over").isFalse();
        assertThat(timingService.hasEnded(match, LocalDateTime.of(2026, 10, 17, 11, 14))).as("one minute before").isFalse();
        assertThat(timingService.hasEnded(match, LocalDateTime.of(2026, 10, 17, 11, 15))).as("at 11:15").isTrue();
        assertThat(timingService.hasEnded(match, LocalDateTime.of(2026, 10, 17, 18, 0))).as("later that day").isTrue();
        assertThat(timingService.hasEnded(match, LocalDateTime.of(2026, 10, 18, 9, 0))).as("the next day").isTrue();
    }

    @Test
    void hasEnded_whenTheMatchIsInTheFuture_thenFalse() {
        eventConfig(60, 15);

        assertThat(timingService.hasEnded(matchAt(TEN_AM.plusDays(3), eventId), TEN_AM)).isFalse();
    }
}
