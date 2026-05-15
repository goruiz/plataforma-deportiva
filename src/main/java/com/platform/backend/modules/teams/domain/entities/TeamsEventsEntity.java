package com.platform.backend.modules.teams.domain.entities;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "teams_events")
public class TeamsEventsEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", foreignKey = @ForeignKey(name = "fk_teams_teams_events_2"))
    private TeamsEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_events_teams_events_1"))
    private EventsEntity event;

    public TeamsEventsEntity() {}

    public TeamsEntity getTeam() { return team; }
    public void setTeam(TeamsEntity team) { this.team = team; }

    public EventsEntity getEvent() { return event; }
    public void setEvent(EventsEntity event) { this.event = event; }
}
