package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IAccountCapabilitiesService;
import com.platform.backend.modules.users.domain.enums.AccountCapability;
import com.platform.backend.modules.users.infrastructure.configuration.security.TeamManagerRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * The one place that says which role gives which power. To give a role something new: add the constant to
 * {@link AccountCapability}, add one line here, and ask for the capability where it matters; the apps already read the
 * whole list. A new role is one more branch of {@link #of(UUID)}.
 */
@Service
@RequiredArgsConstructor
public class AccountCapabilitiesService implements IAccountCapabilitiesService {

    private final TeamManagerRole teamManagerRole;

    @Override
    public Set<AccountCapability> of(UUID userId) {
        Set<AccountCapability> capabilities = EnumSet.noneOf(AccountCapability.class);
        if (teamManagerRole.isTeamManager(userId)) {
            capabilities.add(AccountCapability.OPEN_MATCH_DISPUTES);
        }
        return capabilities;
    }
}
