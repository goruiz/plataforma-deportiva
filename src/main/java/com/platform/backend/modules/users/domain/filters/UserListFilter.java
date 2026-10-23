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
 */
public record UserListFilter(Collection<UUID> withAnyRoleIds, Collection<UUID> withoutAnyRoleIds, List<String> searchTerms) {
}
