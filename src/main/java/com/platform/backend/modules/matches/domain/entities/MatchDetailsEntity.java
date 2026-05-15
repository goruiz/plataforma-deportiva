package com.platform.backend.modules.matches.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "match_details")
public class MatchDetailsEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_match", foreignKey = @ForeignKey(name = "fk_match_details_matches_1"))
    private MatchesEntity match;

    @Column(name = "id_player")
    private UUID idPlayer;

    @Column(name = "id_team")
    private UUID idTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_action", foreignKey = @ForeignKey(name = "fk_match_details_actions_2"))
    private ActionsEntity action;

    @Column
    private Short minute;

    public MatchDetailsEntity() {}

    public MatchesEntity getMatch() { return match; }
    public void setMatch(MatchesEntity match) { this.match = match; }

    public UUID getIdPlayer() { return idPlayer; }
    public void setIdPlayer(UUID idPlayer) { this.idPlayer = idPlayer; }

    public UUID getIdTeam() { return idTeam; }
    public void setIdTeam(UUID idTeam) { this.idTeam = idTeam; }

    public ActionsEntity getAction() { return action; }
    public void setAction(ActionsEntity action) { this.action = action; }

    public Short getMinute() { return minute; }
    public void setMinute(Short minute) { this.minute = minute; }
}
