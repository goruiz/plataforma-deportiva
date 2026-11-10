package com.platform.backend.modules.users.presentation.responses.UserListItemResponse;

import com.platform.backend.modules.users.presentation.responses.UserRoleResponse.UserRoleResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** An account as a row of the users list: its data and its roles, so the client needs no second request for them. */
@Data
public class UserListItemResponse {
    private UUID id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private String username;
    private String email;
    private UUID organizationId;
    /** When the account was suspended; null while it is active. */
    private LocalDateTime suspendedAt;
    private List<UserRoleResponse> roles;
}
