package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "event_schedule_config")
public class EventScheduleConfigEntity extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    /** Comma-separated DayOfWeek names, e.g. "MONDAY,WEDNESDAY,FRIDAY" */
    @Column(name = "play_days", nullable = false, length = 100)
    private String playDays;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "match_duration_minutes", nullable = false)
    private int matchDurationMinutes;

    @Column(name = "break_between_halves_minutes", nullable = false)
    private int breakBetweenHalvesMinutes = 0;

    @Column(name = "break_between_matches_minutes", nullable = false)
    private int breakBetweenMatchesMinutes;

    @Column(name = "court_id")
    private UUID courtId;

    public EventScheduleConfigEntity() {}

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getPlayDays() { return playDays; }
    public void setPlayDays(String playDays) { this.playDays = playDays; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public int getMatchDurationMinutes() { return matchDurationMinutes; }
    public void setMatchDurationMinutes(int matchDurationMinutes) { this.matchDurationMinutes = matchDurationMinutes; }

    public int getBreakBetweenHalvesMinutes() { return breakBetweenHalvesMinutes; }
    public void setBreakBetweenHalvesMinutes(int breakBetweenHalvesMinutes) { this.breakBetweenHalvesMinutes = breakBetweenHalvesMinutes; }

    public int getBreakBetweenMatchesMinutes() { return breakBetweenMatchesMinutes; }
    public void setBreakBetweenMatchesMinutes(int breakBetweenMatchesMinutes) { this.breakBetweenMatchesMinutes = breakBetweenMatchesMinutes; }

    public UUID getCourtId() { return courtId; }
    public void setCourtId(UUID courtId) { this.courtId = courtId; }
}
