package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** What a player sees in the app follows their team, so their account takes the organization of the team they play for. */
@ExtendWith(MockitoExtension.class)
class PlayerAccountOrganizationServiceTest {

    private static final String EMAIL = "ana@email.com";
    private static final UUID ADMIN_ROLE_ID = UUID.randomUUID();
    private static final UUID OWNER_ROLE_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Mock private IUserRepository userRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;

    @InjectMocks
    private PlayerAccountOrganizationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "adminRoleId", ADMIN_ROLE_ID.toString());
    }

    @Test
    void followTeam_whenTheTeamHasAnOrganization_thenTheAccountIsPutInIt() {
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));

        service.followTeam(EMAIL, ORGANIZATION_ID);

        verify(userRepository).assignOrganizationToPlayerAccount(eq(EMAIL), eq(ORGANIZATION_ID), any());
    }

    @Test
    void followTeam_whenThereIsNoTeamOrItHasNoOrganization_thenTheAccountLeavesItsOrganization() {
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));

        service.followTeam(EMAIL, null);

        verify(userRepository).clearOrganizationOfPlayerAccount(eq(EMAIL), any());
    }

    @Test
    void followTeam_whenAnAdministratorOrTheOwnerHoldsTheAccount_thenTheirRolesAreExcluded() {
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));

        service.followTeam(EMAIL, ORGANIZATION_ID);

        ArgumentCaptor<Collection<UUID>> excluded = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository).assignOrganizationToPlayerAccount(anyString(), any(), excluded.capture());
        assertThat(excluded.getValue()).containsExactlyInAnyOrder(ADMIN_ROLE_ID, OWNER_ROLE_ID);
    }

    @Test
    void followTeam_whenNoRolesAreConfigured_thenTheExclusionListIsNeverEmpty() {
        ReflectionTestUtils.setField(service, "adminRoleId", "");
        when(platformOwnerRole.id()).thenReturn(Optional.empty());

        service.followTeam(EMAIL, ORGANIZATION_ID);

        ArgumentCaptor<Collection<UUID>> excluded = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository).assignOrganizationToPlayerAccount(anyString(), any(), excluded.capture());
        assertThat(excluded.getValue()).isNotEmpty();
    }

    @Test
    void followTeam_whenThereIsNoEmail_thenNothingHappens() {
        service.followTeam(null, ORGANIZATION_ID);

        verifyNoInteractions(userRepository);
    }
}
