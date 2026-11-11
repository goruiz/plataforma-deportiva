package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.domain.enums.UserSection;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UserListItemResponse.UserListItemResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;

import java.util.List;
import java.util.UUID;

public interface IUsersManagementService {

    /** One page of the accounts of a section, ordered by name; {@code searchTerms} are lower-cased words that must all appear. */
    PageResponse<UserListItemResponse> getUsers(UserSection section, PageQuery pageQuery, List<String> searchTerms);

    /** How many accounts each section has (for the same search), so the screen can number its tabs without loading them. */
    UserCountsResponse countUsersBySection(List<String> searchTerms);

    /** How many accounts of each section are suspended (they are also in {@link #countUsersBySection}'s numbers). */
    UserCountsResponse countSuspendedBySection();

    UserResponse getUserById(UUID id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    /** Erases the account from the database, with its role assignments and tokens: it is not just marked as deleted. */
    void hardDelete(UUID id);
}
