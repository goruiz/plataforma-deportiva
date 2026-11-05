package com.platform.backend.modules.users.presentation.requests.DeleteAccountRequest;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountRequest {

    /** The account's password: deleting is not undone, so a stolen session alone is not enough to do it. */
    @NotBlank
    private String password;
}
