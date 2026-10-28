package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.filters.UserListFilter;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository {

    UsersEntity save(UsersEntity user);

    /** One page of the active accounts that match the filter, ordered by name, then email. */
    PageResult<UsersEntity> findActivePage(UserListFilter filter, PageQuery pageQuery);

    /** How many active accounts match the filter. */
    long countActive(UserListFilter filter);

    Optional<UsersEntity> findById(UUID id);

    Optional<UsersEntity> findByEmail(String email);

    /** Usernames are unique in the whole platform, so this looks at every organization, not only the caller's. */
    Optional<UsersEntity> findByUsername(String username);

    /** Emails are unique in the whole platform, so this looks at every organization, not only the caller's. */
    boolean existsByEmail(String email);

    boolean existsActiveByOrganizationId(UUID organizationId);

    Optional<UsersEntity> findActiveById(UUID id);

    /**
     * Puts the account with that email in an organization, whatever organization the caller sees; an account that holds
     * one of {@code privilegedRoleIds} is left alone. Returns how many accounts changed.
     */
    int assignOrganizationToPlayerAccount(String email, UUID organizationId, Collection<UUID> privilegedRoleIds);

    /** Takes the account with that email out of its organization; same exceptions as above. */
    int clearOrganizationOfPlayerAccount(String email, Collection<UUID> privilegedRoleIds);

    void hardDelete(UsersEntity user);
}
