package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/**
 * The answer of a player to "will you be at this match?". It takes the organization of its match, like the match
 * events do, and is scoped by the same filter.
 */
@Entity
@Table(name = "match_attendances")
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OF_VISIBLE_MATCH_BY_MATCH_ID, deduceAliasInjectionPoints = false)
public class MatchAttendancesEntity extends BaseEntity {

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    /** The team the player answers for: it stays what it was when they answered, even if they change team later. */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /** A name of {@code AttendanceStatus}, stored as text like the other states. */
    @Column(length = 20, nullable = false)
    private String status;

    public MatchAttendancesEntity() {}

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
