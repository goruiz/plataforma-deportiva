package com.platform.backend.modules.users.infrastructure.configuration.security;

import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamManagerRoleTest {

    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private IUsersRolesRepository usersRolesRepository;

    @InjectMocks
    private TeamManagerRole teamManagerRole;

    @Test
    void anAccountHoldsTheRoleOnlyThroughAnActiveAssignmentOfItsId() {
        ReflectionTestUtils.setField(teamManagerRole, "teamManagerRoleId", ROLE_ID.toString());
        when(usersRolesRepository.existsActiveByUserAndRole(USER_ID, ROLE_ID)).thenReturn(true);

        assertThat(teamManagerRole.isTeamManager(USER_ID)).isTrue();
    }

    @Test
    void anAccountWithoutTheAssignmentIsNotATeamManager() {
        ReflectionTestUtils.setField(teamManagerRole, "teamManagerRoleId", ROLE_ID.toString());
        when(usersRolesRepository.existsActiveByUserAndRole(USER_ID, ROLE_ID)).thenReturn(false);

        assertThat(teamManagerRole.isTeamManager(USER_ID)).isFalse();
    }

    @Test
    void withNoRoleConfiguredNobodyIsATeamManagerAndNothingIsAsked() {
        ReflectionTestUtils.setField(teamManagerRole, "teamManagerRoleId", "");

        assertThat(teamManagerRole.isTeamManager(USER_ID)).isFalse();
        verify(usersRolesRepository, never()).existsActiveByUserAndRole(USER_ID, ROLE_ID);
    }
}
