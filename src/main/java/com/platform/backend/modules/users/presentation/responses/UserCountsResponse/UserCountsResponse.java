package com.platform.backend.modules.users.presentation.responses.UserCountsResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** How many accounts each group of the users screen has (the numbers on its tabs); the three add up to every account. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCountsResponse {
    private long owners;
    private long admins;
    private long appUsers;
}
