package com.platform.backend.modules.players.domain.entities;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "players", uniqueConstraints = {
        @UniqueConstraint(name = "players_email_key", columnNames = "email")
})
public class PlayersEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team", foreignKey = @ForeignKey(name = "fk_teams_players_1"))
    private TeamsEntity team;

    @Column(length = 255, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    public PlayersEntity() {}

    public TeamsEntity getTeam() { return team; }
    public void setTeam(TeamsEntity team) { this.team = team; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
