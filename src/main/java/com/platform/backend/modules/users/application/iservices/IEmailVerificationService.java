package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.presentation.requests.VerifyEmailRequest.VerifyEmailRequest;

public interface IEmailVerificationService {

    /** Emails the account a new confirmation link, replacing the one it had. An email that cannot be sent is an exception. */
    void sendVerificationLink(UsersEntity user);

    /** Sends the link again to the signed-in account; one that is already confirmed has nothing to confirm. */
    void resendVerificationLink(String email);

    /** Confirms the email the link was sent to. */
    void verify(VerifyEmailRequest request);

    /** Refuses with a 403 ({@code EMAIL_NOT_VERIFIED}) unless the account with that email has confirmed it. */
    void requireVerified(String email);
}
