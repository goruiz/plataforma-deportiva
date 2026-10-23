package com.platform.backend.modules.matches.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OF_OWN_EVENT, deduceAliasInjectionPoints = false)
public class MatchesEntity extends BaseEntity {

    @Column(name = "home_team_id", nullable = false)
    private UUID homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private UUID awayTeamId;

    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate;

    @Column(length = 300)
    private String location;

    @Column(length = 20, nullable = false)
    private String status = "SCHEDULED";

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "id_court")
    private UUID courtId;

    // Where the match sits in the championship. A match created by hand (or before championships existed) has none
    // of this and counts as a league match.

    /**
     * Stored by name as plain text on purpose: for a column whose Java type is an enum Hibernate writes a CHECK
     * constraint with its values, and a stage added later would be refused by an existing database.
     * The accessors below still speak {@link MatchStage}.
     */
    @Column(name = "stage", length = 20)
    private String stage;

    /** A league or group matchday, a Swiss round, or a round of the knockout rounds. */
    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "group_number")
    private Integer groupNumber;

    /** Eliminations: which tie of the round this match belongs to (a tie has one match, or two legs). */
    @Column(name = "tie_number")
    private Integer tieNumber;

    @Column(name = "leg_number")
    private Integer legNumber;

    /** When a tie ends level on goals: the team an administrator says advances (set on the match that closes the tie). */
    @Column(name = "advancing_team_id")
    private UUID advancingTeamId;

    public MatchesEntity() {}

    public UUID getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(UUID homeTeamId) { this.homeTeamId = homeTeamId; }

    public UUID getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(UUID awayTeamId) { this.awayTeamId = awayTeamId; }

    public LocalDateTime getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getCourtId() { return courtId; }
    public void setCourtId(UUID courtId) { this.courtId = courtId; }

    /** The stage the match was generated for; null for a match without one. */
    public MatchStage getStage() { return stage == null ? null : MatchStage.valueOf(stage); }
    public void setStage(MatchStage stage) { this.stage = stage == null ? null : stage.name(); }

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }

    public Integer getGroupNumber() { return groupNumber; }
    public void setGroupNumber(Integer groupNumber) { this.groupNumber = groupNumber; }

    public Integer getTieNumber() { return tieNumber; }
    public void setTieNumber(Integer tieNumber) { this.tieNumber = tieNumber; }

    public Integer getLegNumber() { return legNumber; }
    public void setLegNumber(Integer legNumber) { this.legNumber = legNumber; }

    public UUID getAdvancingTeamId() { return advancingTeamId; }
    public void setAdvancingTeamId(UUID advancingTeamId) { this.advancingTeamId = advancingTeamId; }
}
