package com.platform.backend.modules.users.presentation.responses.AccountsByOrganizationResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** The accounts of one organization; the line without an organization is the platform owner's own. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountsByOrganizationResponse {
    private UUID organizationId;
    /** Null for the platform owner's line, and for an organization that no longer exists. */
    private String organizationName;
    private long accounts;
}
