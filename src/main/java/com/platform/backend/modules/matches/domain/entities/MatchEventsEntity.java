package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "match_events")
public class MatchEventsEntity extends BaseEntity {

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "event_type", length = 30, nullable = false)
    private String eventType;

    @Column
    private Integer minute;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public MatchEventsEntity() {}

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
