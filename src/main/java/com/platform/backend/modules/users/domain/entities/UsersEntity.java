package com.platform.backend.modules.users.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_ORGANIZATION_OR_SELF, deduceAliasInjectionPoints = false)
public class UsersEntity extends BaseEntity implements HasUniqueField, OrganizationOwned {

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "middle_name", length = 255)
    private String middleName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "second_last_name", length = 255)
    private String secondLastName;

    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

    /** When the owner of the email confirmed it; null while it is unconfirmed. */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /**
     * A token issued before this moment no longer opens a session: signing out or deleting the account sets it. It is
     * ignored when the entity becomes the claims of a token (see {@code JwtService#generateToken(Object, String)}).
     */
    @JsonIgnore
    @Column(name = "tokens_valid_after")
    private LocalDateTime tokensValidAfter;

    /** When an administrator suspended the account; null while it is active. A suspended account cannot sign in. */
    @JsonIgnore
    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    /** {@code MALE} or {@code FEMALE} ({@code Gender}); null until the person says it. Kept in the player too. */
    @JsonIgnore
    @Column(length = 10)
    private String gender;

    /** Null until the person says it. Kept in the player too. */
    @JsonIgnore
    @Column(name = "birth_date")
    private LocalDate birthDate;

    public UsersEntity() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSecondLastName() { return secondLastName; }
    public void setSecondLastName(String secondLastName) { this.secondLastName = secondLastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public boolean isEmailVerified() { return emailVerifiedAt != null; }

    @JsonIgnore
    public LocalDateTime getTokensValidAfter() { return tokensValidAfter; }
    public void setTokensValidAfter(LocalDateTime tokensValidAfter) { this.tokensValidAfter = tokensValidAfter; }

    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(LocalDateTime suspendedAt) { this.suspendedAt = suspendedAt; }

    public boolean isSuspended() { return suspendedAt != null; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.email != null) {
            this.email = this.email + "_DELETED_" + entityId;
        }
    }

    @Override
    public UUID getOrganizationId() { return organizationId; }

    @Override
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
