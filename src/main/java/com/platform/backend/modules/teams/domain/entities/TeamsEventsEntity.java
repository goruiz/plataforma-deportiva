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

    /** The group the team was drawn into, in an event whose format has a group stage. */
    @Column(name = "group_number")
    private Integer groupNumber;

    /** The team's place when the elimination rounds started (1 is the best), used to build the bracket. */
    @Column(name = "seed")
    private Integer seed;

    public TeamsEventsEntity() {}

    public TeamsEntity getTeam() { return team; }
    public void setTeam(TeamsEntity team) { this.team = team; }

    public EventsEntity getEvent() { return event; }
    public void setEvent(EventsEntity event) { this.event = event; }

    public Integer getGroupNumber() { return groupNumber; }
    public void setGroupNumber(Integer groupNumber) { this.groupNumber = groupNumber; }

    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
}
