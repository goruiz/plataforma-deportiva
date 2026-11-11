package com.platform.backend.modules.users.domain.enums;

/**
 * Something an account may do beyond what every user of the app does. The code (and the apps) ask for the capability, not
 * for the role that happens to grant it, so a new role or a new power is one line in {@code AccountCapabilitiesService}
 * and one constant here. The constant's name is what the apps receive (in {@code capabilities} of {@code GET
 * /users/auth/me}), so it is stable.
 */
public enum AccountCapability {
    /**
     * Tells the organizers that the result of a match of the team looks wrong (a "dispute"), and sees the answer. Held by
     * the "Dirigente de equipo" role; it still needs the account to belong to the team it speaks for.
     */
    OPEN_MATCH_DISPUTES
}
