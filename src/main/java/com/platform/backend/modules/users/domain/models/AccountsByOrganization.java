package com.platform.backend.modules.users.domain.models;

import java.util.UUID;

/** How many accounts belong to one organization; a null {@code organizationId} is the platform owner's own. */
public record AccountsByOrganization(UUID organizationId, long accounts) {
}
