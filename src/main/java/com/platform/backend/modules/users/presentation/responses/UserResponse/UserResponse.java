package com.platform.backend.modules.users.presentation.responses.UserResponse;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private String username;
    private String email;
    private UUID organizationId;
    /** Whether the owner of the email has confirmed it. */
    private boolean emailVerified;
    /** When the account was suspended; null while it is active. */
    private LocalDateTime suspendedAt;
    /** {@code MALE} or {@code FEMALE}; null until the person says it. */
    private String gender;
    /** Null until the person says it. */
    private LocalDate birthDate;
    /**
     * What the account may do beyond the basics ({@code AccountCapability} names), from the roles it holds. The apps show
     * a feature only to whoever has its capability. Empty for an ordinary user.
     */
    private List<String> capabilities = List.of();
}
