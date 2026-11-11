package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.domain.enums.AccountCapability;
import com.platform.backend.modules.users.infrastructure.configuration.security.TeamManagerRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountCapabilitiesServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private TeamManagerRole teamManagerRole;

    @InjectMocks
    private AccountCapabilitiesService service;

    @Test
    void aTeamManagerMayOpenMatchDisputes() {
        when(teamManagerRole.isTeamManager(USER_ID)).thenReturn(true);

        assertThat(service.of(USER_ID)).containsExactly(AccountCapability.OPEN_MATCH_DISPUTES);
        assertThat(service.has(USER_ID, AccountCapability.OPEN_MATCH_DISPUTES)).isTrue();
    }

    @Test
    void anOrdinaryUserHasNoExtraPowers() {
        when(teamManagerRole.isTeamManager(USER_ID)).thenReturn(false);

        assertThat(service.of(USER_ID)).isEmpty();
        assertThat(service.has(USER_ID, AccountCapability.OPEN_MATCH_DISPUTES)).isFalse();
    }
}
