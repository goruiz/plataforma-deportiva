package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.DeleteAccountRequest.DeleteAccountRequest;

public interface IAccountLifecycleService {

    /** Ends the account sessions: every token issued until now stops working, on every device. */
    void signOut(String email);

    /**
     * Deletes the account of a player (and their player), erasing the personal data. The account of an administrator
     * or of the platform owner is refused: those are removed from the panel.
     */
    void deleteAccount(DeleteAccountRequest request, String email);
}
