package com.platform.backend.modules.matches.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.enums.MatchStatus;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OF_VISIBLE_EVENT, deduceAliasInjectionPoints = false)
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

    /**
     * The penalty shoot-out that settled a result that ended level (the goals of the shoot-out, not of the match).
     * Only kept while the score is level and the organization's settings allow penalties.
     */
    @Column(name = "home_penalty_score")
    private Integer homePenaltyScore;

    @Column(name = "away_penalty_score")
    private Integer awayPenaltyScore;

    /** When the current result was recorded (or last corrected): a dispute window counts from here. */
    @Column(name = "result_recorded_at")
    private LocalDateTime resultRecordedAt;

    /** When an administrator confirmed the result as official; null while it is not, and again after it changes. */
    @Column(name = "result_validated_at")
    private LocalDateTime resultValidatedAt;

    @Column(name = "result_validated_by")
    private UUID resultValidatedBy;

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

    public Integer getHomePenaltyScore() { return homePenaltyScore; }
    public void setHomePenaltyScore(Integer homePenaltyScore) { this.homePenaltyScore = homePenaltyScore; }

    public Integer getAwayPenaltyScore() { return awayPenaltyScore; }
    public void setAwayPenaltyScore(Integer awayPenaltyScore) { this.awayPenaltyScore = awayPenaltyScore; }

    public LocalDateTime getResultRecordedAt() { return resultRecordedAt; }
    public void setResultRecordedAt(LocalDateTime resultRecordedAt) { this.resultRecordedAt = resultRecordedAt; }

    public LocalDateTime getResultValidatedAt() { return resultValidatedAt; }
    public void setResultValidatedAt(LocalDateTime resultValidatedAt) { this.resultValidatedAt = resultValidatedAt; }

    public UUID getResultValidatedBy() { return resultValidatedBy; }
    public void setResultValidatedBy(UUID resultValidatedBy) { this.resultValidatedBy = resultValidatedBy; }

    /** Whether the result was settled by a penalty shoot-out (both penalty scores are present). */
    public boolean hasPenaltyShootout() {
        return homePenaltyScore != null && awayPenaltyScore != null;
    }

    /** A match counts as played once it has a result (both scores), whatever its status says: as in the championship. */
    public boolean hasResult() {
        return homeScore != null && awayScore != null;
    }

    /** Whether the team plays this match, at home or away. */
    public boolean isPlayedBy(UUID teamId) {
        return teamId != null && (teamId.equals(homeTeamId) || teamId.equals(awayTeamId));
    }

    /**
     * A match has started once its kick-off time has passed. A postponed one never was played, so it counts as not
     * started until it is rescheduled.
     */
    public boolean hasStarted(LocalDateTime now) {
        return !matchDate.isAfter(now) && !MatchStatus.POSTPONED.name().equals(status);
    }
}
