package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UsersManagementServiceTest {

    private static final UUID ADMIN_ROLE_ID = UUID.fromString("4fd36dd3-c40a-4063-9e88-fed84872564f");

    @Mock private IUserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsersManagementService usersManagementService;

    private UsersEntity userEntity;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userEntity = new UsersEntity();
        userEntity.setEmail("juan@email.com");

        userResponse = new UserResponse();
        userResponse.setEmail("juan@email.com");
    }

    // ─── getAdminUsers ──────────────────────────────────────────────────────────

    @Test
    void getAdminUsers_whenAdminRoleConfigured_thenReturnUsersHoldingThatRole() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        when(userRepository.findAllActiveWithRole(ADMIN_ROLE_ID)).thenReturn(List.of(userEntity));
        when(userMapper.toUserResponse(userEntity)).thenReturn(userResponse);

        List<UserResponse> admins = usersManagementService.getAdminUsers();

        assertThat(admins).containsExactly(userResponse);
    }

    @Test
    void getAdminUsers_whenAdminRoleNotConfigured_thenReturnEmptyWithoutQueryingRepository() {
        configureAdminRoleId("");

        List<UserResponse> admins = usersManagementService.getAdminUsers();

        assertThat(admins).isEmpty();
        verify(userRepository, never()).findAllActiveWithRole(any());
    }

    // ─── getRegularUsers ────────────────────────────────────────────────────────

    @Test
    void getRegularUsers_whenAdminRoleConfigured_thenReturnUsersWithoutThatRole() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        when(userRepository.findAllActiveWithoutRole(ADMIN_ROLE_ID)).thenReturn(List.of(userEntity));
        when(userMapper.toUserResponse(userEntity)).thenReturn(userResponse);

        List<UserResponse> regularUsers = usersManagementService.getRegularUsers();

        assertThat(regularUsers).containsExactly(userResponse);
        verify(userRepository, never()).findAllActive();
    }

    @Test
    void getRegularUsers_whenAdminRoleNotConfigured_thenReturnEveryActiveUser() {
        configureAdminRoleId("");
        when(userRepository.findAllActive()).thenReturn(List.of(userEntity));
        when(userMapper.toUserResponse(userEntity)).thenReturn(userResponse);

        List<UserResponse> regularUsers = usersManagementService.getRegularUsers();

        assertThat(regularUsers).containsExactly(userResponse);
        verify(userRepository, never()).findAllActiveWithoutRole(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void configureAdminRoleId(String adminRoleId) {
        ReflectionTestUtils.setField(usersManagementService, "adminRoleId", adminRoleId);
    }
}
