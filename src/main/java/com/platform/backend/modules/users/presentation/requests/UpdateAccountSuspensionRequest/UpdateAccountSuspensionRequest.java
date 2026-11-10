package com.platform.backend.modules.users.presentation.requests.UpdateAccountSuspensionRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAccountSuspensionRequest {

    /** True suspends the account (it cannot sign in and its open sessions end); false reactivates it. */
    @NotNull
    private Boolean suspended;
}
