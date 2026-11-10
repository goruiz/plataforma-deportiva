package com.platform.backend.modules.settings.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The match rules of one organization. The row without an organization holds the platform defaults, which every
 * organization that has no row of its own inherits.
 * <p>
 * It has no tenancy filter on purpose: an administrator has to read the platform row when their organization has none.
 * Who may read or write which row is decided by {@code SystemSettingsService}.
 */
@Entity
@Table(name = "system_settings")
public class SystemSettingsEntity extends BaseEntity {

    /** The organization these rules are for; null for the platform defaults. */
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "match_wait_minutes", nullable = false)
    private int matchWaitMinutes;

    @Column(name = "max_substitutions", nullable = false)
    private int maxSubstitutions;

    @Column(name = "draws_allowed", nullable = false)
    private boolean drawsAllowed;

    @Column(name = "extra_time_minutes", nullable = false)
    private int extraTimeMinutes;

    @Column(name = "penalties_enabled", nullable = false)
    private boolean penaltiesEnabled;

    @Column(name = "dispute_window_hours", nullable = false)
    private int disputeWindowHours;

    public SystemSettingsEntity() {}

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public int getMatchWaitMinutes() { return matchWaitMinutes; }
    public void setMatchWaitMinutes(int matchWaitMinutes) { this.matchWaitMinutes = matchWaitMinutes; }

    public int getMaxSubstitutions() { return maxSubstitutions; }
    public void setMaxSubstitutions(int maxSubstitutions) { this.maxSubstitutions = maxSubstitutions; }

    public boolean isDrawsAllowed() { return drawsAllowed; }
    public void setDrawsAllowed(boolean drawsAllowed) { this.drawsAllowed = drawsAllowed; }

    public int getExtraTimeMinutes() { return extraTimeMinutes; }
    public void setExtraTimeMinutes(int extraTimeMinutes) { this.extraTimeMinutes = extraTimeMinutes; }

    public boolean isPenaltiesEnabled() { return penaltiesEnabled; }
    public void setPenaltiesEnabled(boolean penaltiesEnabled) { this.penaltiesEnabled = penaltiesEnabled; }

    public int getDisputeWindowHours() { return disputeWindowHours; }
    public void setDisputeWindowHours(int disputeWindowHours) { this.disputeWindowHours = disputeWindowHours; }
}
