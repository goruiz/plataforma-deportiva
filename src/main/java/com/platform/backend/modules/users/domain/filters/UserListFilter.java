package com.platform.backend.modules.users.domain.filters;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * What a list of active accounts is narrowed by. An empty collection means "no condition of that kind".
 *
 * @param withAnyRoleIds    the account must hold at least one of these roles
 * @param withoutAnyRoleIds the account must hold none of these roles
 * @param searchTerms       lower-cased words; each one must appear in the account's name, username or email
 * @param suspendedOnly     only the accounts an administrator suspended (the users report counts them apart)
 */
public record UserListFilter(Collection<UUID> withAnyRoleIds, Collection<UUID> withoutAnyRoleIds, List<String> searchTerms,
                             boolean suspendedOnly) {

    /** Every account the other conditions allow, suspended or not: what the lists and their counts ask for. */
    public UserListFilter(Collection<UUID> withAnyRoleIds, Collection<UUID> withoutAnyRoleIds, List<String> searchTerms) {
        this(withAnyRoleIds, withoutAnyRoleIds, searchTerms, false);
    }

    /** The same conditions, narrowed to the suspended accounts. */
    public UserListFilter onlySuspended() {
        return new UserListFilter(withAnyRoleIds, withoutAnyRoleIds, searchTerms, true);
    }
}
