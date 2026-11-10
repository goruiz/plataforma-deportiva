package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;

import java.util.UUID;

public interface IAccountSuspensionService {

    /**
     * Suspends or reactivates an account of the caller's organization (the platform owner sees them all). A suspended
     * account keeps everything it has (roles, team, history) but cannot sign in, and the sessions it had stop working
     * at once, also after it is reactivated. Asking for the state the account already has changes nothing.
     */
    UserResponse changeSuspension(UUID userId, boolean suspended);
}
