package com.platform.backend.modules.players.domain.entities;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One team a player plays for. A player (the person) can play for several teams, but only one per organization: an
 * administrator sees the memberships of their own organization's teams, and the player sees all of theirs.
 * <p>
 * Whoever leaves a team keeps the row, marked as deleted: the history of who played where stays, and the unique indexes
 * only look at the rows still in force.
 */
@Entity
@Table(name = "team_members")
@Filter(name = TenantFilters.NAME, condition = TenantFilters.MEMBER_OF_OWN_TEAM_OR_SELF, deduceAliasInjectionPoints = false)
public class TeamMembersEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private PlayersEntity player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private TeamsEntity team;

    /** The team's organization, copied so that "one team per organization" can be a unique index; null for the owner's. */
    @Column(name = "organization_id")
    private UUID organizationId;

    /** Captain of this team; the administrator designates it and there is at most one per team. */
    @Column(name = "is_captain", nullable = false)
    private Boolean isCaptain = false;

    /** Manages this team (its join code) without being an administrator of the platform; a team can have several. */
    @Column(name = "is_team_admin", nullable = false)
    private Boolean isTeamAdmin = false;

    public TeamMembersEntity() {}

    /** A membership starts as an ordinary player of the team: the captaincy is designated apart. */
    public static TeamMembersEntity of(PlayersEntity player, TeamsEntity team) {
        TeamMembersEntity membership = new TeamMembersEntity();
        membership.player = player;
        membership.team = team;
        membership.organizationId = team.getOrganizationId();
        return membership;
    }

    public PlayersEntity getPlayer() { return player; }
    public void setPlayer(PlayersEntity player) { this.player = player; }

    public TeamsEntity getTeam() { return team; }
    public void setTeam(TeamsEntity team) { this.team = team; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public Boolean getIsCaptain() { return isCaptain; }
    public void setIsCaptain(Boolean isCaptain) { this.isCaptain = isCaptain; }

    public boolean isCaptainOfTheTeam() { return Boolean.TRUE.equals(isCaptain); }

    public Boolean getIsTeamAdmin() { return isTeamAdmin; }
    public void setIsTeamAdmin(Boolean isTeamAdmin) { this.isTeamAdmin = isTeamAdmin; }

    public boolean isAdministratorOfTheTeam() { return Boolean.TRUE.equals(isTeamAdmin); }

    /** The player stops playing for the team; a captain or an administrator of the team stops being one with it. */
    public void end() {
        isCaptain = false;
        isTeamAdmin = false;
        setDeletedAt(LocalDateTime.now());
    }
}
