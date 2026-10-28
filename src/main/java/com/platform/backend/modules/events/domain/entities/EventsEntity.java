package com.platform.backend.modules.events.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "events")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_OR_PUBLIC_EVENT, deduceAliasInjectionPoints = false)
public class EventsEntity extends BaseTranslatableEntity implements HasUniqueField, OrganizationOwned {

    @Column(length = 200)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 20)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "url_logo", length = 255)
    private String urlLogo;

    @Column(name = "min_players_per_team")
    private Integer minPlayersPerTeam;

    @Column(name = "max_players_per_team")
    private Integer maxPlayersPerTeam;

    @Column(name = "min_teams")
    private Integer minTeams;

    @Column(name = "max_teams")
    private Integer maxTeams;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_event_type", foreignKey = @ForeignKey(name = "fk_events_types_events"))
    private EventTypesEntity eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_event_format", foreignKey = @ForeignKey(name = "fk_events_formats_events"))
    private EventFormatsEntity eventFormat;

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

    /** A public event is seen by every player in the app, whatever their organization; the organizer decides it. */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    public EventsEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getMinPlayersPerTeam() {
        return minPlayersPerTeam;
    }

    public void setMinPlayersPerTeam(Integer minPlayersPerTeam) {
        this.minPlayersPerTeam = minPlayersPerTeam;
    }

    public Integer getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }

    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }

    public Integer getMinTeams() {
        return minTeams;
    }

    public void setMinTeams(Integer minTeams) {
        this.minTeams = minTeams;
    }

    public Integer getMaxTeams() {
        return maxTeams;
    }

    public void setMaxTeams(Integer maxTeams) {
        this.maxTeams = maxTeams;
    }

    public EventTypesEntity getEventType() {
        return eventType;
    }

    public void setEventType(EventTypesEntity eventType) {
        this.eventType = eventType;
    }

    public EventFormatsEntity getEventFormat() {
        return eventFormat;
    }

    public void setEventFormat(EventFormatsEntity eventFormat) {
        this.eventFormat = eventFormat;
    }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }

    @Override
    public UUID getOrganizationId() { return organizationId; }

    @Override
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public Boolean getIsPublic() { return isPublic; }

    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
