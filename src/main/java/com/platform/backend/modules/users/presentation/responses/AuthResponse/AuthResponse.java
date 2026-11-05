package com.platform.backend.modules.users.presentation.responses.AuthResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private boolean admin;
    /** The platform owner: sees every organization. The panel uses it to show the owner-only screens. */
    private boolean platformOwner;
    /** The organization the account works for; null for the platform owner and for mobile-app accounts. */
    private UUID organizationId;
    private UUID id;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    /** False until the owner of the email confirms it; the app asks for it before joining a team. */
    private boolean emailVerified;
}
