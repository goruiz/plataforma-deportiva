package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.domain.enums.AccountCapability;

import java.util.Set;
import java.util.UUID;

/** What an account may do beyond the basics, according to the roles it holds. */
public interface IAccountCapabilitiesService {

    Set<AccountCapability> of(UUID userId);

    default boolean has(UUID userId, AccountCapability capability) {
        return of(userId).contains(capability);
    }
}
