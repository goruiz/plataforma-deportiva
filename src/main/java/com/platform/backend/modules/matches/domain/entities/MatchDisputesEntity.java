package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.modules.matches.domain.enums.DisputeStatus;
import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A team's complaint against the result of a match. The account that opened it is the record's {@code createdBy}; its
 * name is kept next to it because accounts can be deleted for good and the dispute must still say who raised it.
 * It belongs to the organization of its match's event, through the match.
 */
@Entity
@Table(name = "match_disputes")
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OF_VISIBLE_MATCH_BY_MATCH_ID, deduceAliasInjectionPoints = false)
public class MatchDisputesEntity extends BaseEntity {

    public static final int REASON_MAX_LENGTH = 1000;
    public static final int NOTE_MAX_LENGTH = 1000;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    /** The team that disputes (one of the two that played). */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "opened_by_name", length = 255)
    private String openedByName;

    @Column(length = REASON_MAX_LENGTH, nullable = false)
    private String reason;

    /** The result the team says is the right one, when it proposes one. */
    @Column(name = "proposed_home_score")
    private Integer proposedHomeScore;

    @Column(name = "proposed_away_score")
    private Integer proposedAwayScore;

    /** Stored by name as plain text; the accessors speak {@link DisputeStatus}. */
    @Column(length = 20, nullable = false)
    private String status = DisputeStatus.OPEN.name();

    @Column(name = "resolution_note", length = NOTE_MAX_LENGTH)
    private String resolutionNote;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_by_name", length = 255)
    private String resolvedByName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public MatchDisputesEntity() {}

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getOpenedByName() { return openedByName; }
    public void setOpenedByName(String openedByName) { this.openedByName = openedByName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getProposedHomeScore() { return proposedHomeScore; }
    public void setProposedHomeScore(Integer proposedHomeScore) { this.proposedHomeScore = proposedHomeScore; }

    public Integer getProposedAwayScore() { return proposedAwayScore; }
    public void setProposedAwayScore(Integer proposedAwayScore) { this.proposedAwayScore = proposedAwayScore; }

    public DisputeStatus getStatus() { return DisputeStatus.parse(status).orElse(DisputeStatus.OPEN); }
    public void setStatus(DisputeStatus status) { this.status = status.name(); }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolvedByName() { return resolvedByName; }
    public void setResolvedByName(String resolvedByName) { this.resolvedByName = resolvedByName; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public boolean isOpen() {
        return getStatus() == DisputeStatus.OPEN;
    }
}
