package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events_formats")
public class EventFormatsEntity extends BaseTranslatableEntity implements HasUniqueField {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    // The rules below are null in formats created before rules existed (no structure) and, for a structure,
    // in every rule that structure does not use: EventFormatRulesService keeps them that way.

    /**
     * Stored by name as plain text on purpose: Hibernate writes a CHECK constraint listing the values of any
     * column whose Java type is an enum (also with {@code @Convert}), and with {@code ddl-auto=update} that
     * constraint is never updated, so every structure added later would be refused by the database.
     * The accessors below still speak {@link EventFormatStructure}.
     */
    @Column(name = "structure", length = 30)
    private String structure;

    /** Home and away legs in the league stage (or in each group). */
    @Column(name = "round_trip")
    private Boolean roundTrip;

    @Column(name = "points_for_win")
    private Integer pointsForWin;

    @Column(name = "points_for_draw")
    private Integer pointsForDraw;

    @Column(name = "points_for_loss")
    private Integer pointsForLoss;

    /** Comma-separated TiebreakerCriterion names in priority order, e.g. "GOAL_DIFFERENCE,GOALS_FOR". */
    @Column(name = "tiebreakers", length = 255)
    private String tiebreakers;

    @Column(name = "swiss_rounds")
    private Integer swissRounds;

    @Column(name = "group_count")
    private Integer groupCount;

    @Column(name = "teams_advancing_per_group")
    private Integer teamsAdvancingPerGroup;

    /** Teams that leave the league (or the Swiss rounds) for the knockout rounds. */
    @Column(name = "playoff_teams")
    private Integer playoffTeams;

    /** Home and away legs in each knockout tie. */
    @Column(name = "knockout_round_trip")
    private Boolean knockoutRoundTrip;

    @Column(name = "third_place_match")
    private Boolean thirdPlaceMatch;

    /** Double elimination: whoever comes from the losers' bracket and wins the grand final forces a second one. */
    @Column(name = "grand_final_reset")
    private Boolean grandFinalReset;

    @OneToMany(mappedBy = "eventFormat", fetch = FetchType.LAZY)
    private List<EventsEntity> events;

    public EventFormatsEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public EventFormatStructure getStructure() { return EventFormatStructure.fromStored(structure); }
    public void setStructure(EventFormatStructure structure) { this.structure = structure == null ? null : structure.name(); }

    public Boolean getRoundTrip() { return roundTrip; }
    public void setRoundTrip(Boolean roundTrip) { this.roundTrip = roundTrip; }

    public Integer getPointsForWin() { return pointsForWin; }
    public void setPointsForWin(Integer pointsForWin) { this.pointsForWin = pointsForWin; }

    public Integer getPointsForDraw() { return pointsForDraw; }
    public void setPointsForDraw(Integer pointsForDraw) { this.pointsForDraw = pointsForDraw; }

    public Integer getPointsForLoss() { return pointsForLoss; }
    public void setPointsForLoss(Integer pointsForLoss) { this.pointsForLoss = pointsForLoss; }

    public String getTiebreakers() { return tiebreakers; }
    public void setTiebreakers(String tiebreakers) { this.tiebreakers = tiebreakers; }

    public Integer getSwissRounds() { return swissRounds; }
    public void setSwissRounds(Integer swissRounds) { this.swissRounds = swissRounds; }

    public Integer getGroupCount() { return groupCount; }
    public void setGroupCount(Integer groupCount) { this.groupCount = groupCount; }

    public Integer getTeamsAdvancingPerGroup() { return teamsAdvancingPerGroup; }
    public void setTeamsAdvancingPerGroup(Integer teamsAdvancingPerGroup) { this.teamsAdvancingPerGroup = teamsAdvancingPerGroup; }

    public Integer getPlayoffTeams() { return playoffTeams; }
    public void setPlayoffTeams(Integer playoffTeams) { this.playoffTeams = playoffTeams; }

    public Boolean getKnockoutRoundTrip() { return knockoutRoundTrip; }
    public void setKnockoutRoundTrip(Boolean knockoutRoundTrip) { this.knockoutRoundTrip = knockoutRoundTrip; }

    public Boolean getThirdPlaceMatch() { return thirdPlaceMatch; }
    public void setThirdPlaceMatch(Boolean thirdPlaceMatch) { this.thirdPlaceMatch = thirdPlaceMatch; }

    public Boolean getGrandFinalReset() { return grandFinalReset; }
    public void setGrandFinalReset(Boolean grandFinalReset) { this.grandFinalReset = grandFinalReset; }

    public List<EventsEntity> getEvents() { return events; }
    public void setEvents(List<EventsEntity> events) { this.events = events; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }
}
