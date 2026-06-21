package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventsEntity extends BaseTranslatableEntity implements HasUniqueField {

    @Column(length = 200)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 30)
    private String format;

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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
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

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }
}
