package com.platform.backend.modules.users.application.iservices;

import java.util.UUID;

/**
 * A player's account belongs to the organization of their team: that is what the app shows them. Whoever changes a
 * player's team (the invitation, the admin panel) keeps the account in step through this.
 */
public interface IPlayerAccountOrganizationService {

    /**
     * Puts the account with that email in the organization of the team they now play for; a team with no organization
     * (the platform owner's) or none leaves the account without one. An administrator's account is never changed.
     */
    void followTeam(String email, UUID teamOrganizationId);
}
