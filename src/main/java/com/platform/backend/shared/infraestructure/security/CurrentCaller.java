package com.platform.backend.shared.infraestructure.security;

import com.platform.backend.shared.domain.security.CallerPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the account that is calling from the security context, for what only makes sense "for me". */
public final class CurrentCaller {

    private CurrentCaller() {}

    /** The caller of an endpoint that needs one; the protected endpoints always have it. */
    public static CallerPrincipal require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CallerPrincipal caller) {
            return caller;
        }
        throw new AccessDeniedException("Authentication required");
    }
}
