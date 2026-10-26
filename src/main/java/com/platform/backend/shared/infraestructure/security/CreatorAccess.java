package com.platform.backend.shared.infraestructure.security;

import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.domain.security.CallerPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Who may change the records a mobile-app account creates (a league, a team, the matches of a league).
 * <p>
 * Only an administrator, or the account that created the record, may change it; anyone else can read it but gets a
 * 403. Code that runs without a user (scheduled jobs, public endpoints) is not restricted, like in
 * {@code CurrentTenant}. The organization an administrator works for is already enforced by the tenancy filter, so
 * "administrator" here is enough.
 */
public final class CreatorAccess {

    private CreatorAccess() {}

    /** For a record the caller already holds: pass who created it. */
    public static void requireAdminOrCreator(UUID creatorId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CallerPrincipal caller)) {
            return;
        }
        if (isAdministrator(authentication) || isCreator(caller, creatorId)) {
            return;
        }
        throw new AccessDeniedException(AccessError.NOT_THE_CREATOR.name());
    }

    /**
     * For a record the caller only references by id (the event of a match): looks it up with the repository method
     * and checks its creator. The lookup only runs for a caller who is not an administrator. A missing record has no
     * creator, so nobody but an administrator may change what hangs from it.
     */
    public static void requireAdminOrCreator(UUID recordId, Function<UUID, Optional<? extends BaseEntity>> findActiveById) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CallerPrincipal)
                || isAdministrator(authentication)) {
            return;
        }
        UUID creatorId = recordId == null
                ? null
                : findActiveById.apply(recordId).map(BaseEntity::getCreatedBy).orElse(null);
        requireAdminOrCreator(creatorId);
    }

    private static boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Authority.ADMIN.roleName().equals(authority.getAuthority()));
    }

    private static boolean isCreator(CallerPrincipal caller, UUID creatorId) {
        return creatorId != null && creatorId.equals(caller.getId());
    }
}
