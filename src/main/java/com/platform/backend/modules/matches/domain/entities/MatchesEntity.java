package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class MatchesEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_event", foreignKey = @ForeignKey(name = "fk_matches_events_1"))
    private EventsEntity event;

    @Column(name = "id_home_team", nullable = false)
    private UUID idHomeTeam;

    @Column(name = "id_away_team", nullable = false)
    private UUID idAwayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_court", foreignKey = @ForeignKey(name = "fk_courts_matches_1"))
    private CourtsEntity court;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(length = 20, nullable = false)
    private String status = "SCHEDULED";

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "date")
    private LocalDateTime date;

    public MatchesEntity() {}

    public EventsEntity getEvent() { return event; }
    public void setEvent(EventsEntity event) { this.event = event; }

    public UUID getIdHomeTeam() { return idHomeTeam; }
    public void setIdHomeTeam(UUID idHomeTeam) { this.idHomeTeam = idHomeTeam; }

    public UUID getIdAwayTeam() { return idAwayTeam; }
    public void setIdAwayTeam(UUID idAwayTeam) { this.idAwayTeam = idAwayTeam; }

    public CourtsEntity getCourt() { return court; }
    public void setCourt(CourtsEntity court) { this.court = court; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
