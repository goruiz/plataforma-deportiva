package com.platform.backend.modules.players.domain.enums;

/** Who an invitation is for. The column stays free text. */
public enum InvitationType {
    /** The person already has an account: the invitation is informative and they accept it from the app. */
    EXISTING_USER,
    /** The person has no account yet: the invitation carries a token that the registration link uses. */
    NEW_USER
}
