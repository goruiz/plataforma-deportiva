package com.platform.backend.modules.users.presentation.responses.UserRoleResponse;

import lombok.Data;

import java.util.UUID;

/** A role of an account as a list row shows it: just enough to draw its badge. */
@Data
public class UserRoleResponse {
    private UUID id;
    private String name;
}
