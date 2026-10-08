package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.security.password.TemporaryPasswordGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UsersManagementServiceTest {

    private static final UUID ADMIN_ROLE_ID = UUID.fromString("4fd36dd3-c40a-4063-9e88-fed84872564f");
    private static final String TEMPORARY_PASSWORD = "Temp1234Abcd";
    private static final String ENCODED_PASSWORD = "encoded_temporary_password";

    @Mock private IUserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TemporaryPasswordGenerator temporaryPasswordGenerator;
    @Mock private EmailService emailService;

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

    // ─── createUser ─────────────────────────────────────────────────────────────

    @Test
    void createUser_whenDataIsValid_thenStoreEncodedTemporaryPasswordAndEmailItToTheUser() {
        CreateUserRequest request = buildCreateUserRequest();
        userEntity.setFirstName("Juan");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn(TEMPORARY_PASSWORD);
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(passwordEncoder.encode(TEMPORARY_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userMapper.toUserResponse(userEntity)).thenReturn(userResponse);

        UserResponse createdUser = usersManagementService.createUser(request);

        assertThat(createdUser).isSameAs(userResponse);
        assertThat(userEntity.getPassword()).isEqualTo(ENCODED_PASSWORD);
        verify(emailService).sendUserCreatedWithTemporaryPassword("juan@email.com", "Juan", TEMPORARY_PASSWORD);
    }

    @Test
    void createUser_whenEmailAlreadyInUse_thenFailWithoutSavingNorEmailing() {
        CreateUserRequest request = buildCreateUserRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> usersManagementService.createUser(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void createUser_whenUsernameAlreadyInUse_thenFailWithoutSavingNorEmailing() {
        CreateUserRequest request = buildCreateUserRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(userEntity));

        assertThatThrownBy(() -> usersManagementService.createUser(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void createUser_whenEmailCannotBeSent_thenPropagateErrorSoTheTransactionRollsBack() {
        CreateUserRequest request = buildCreateUserRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn(TEMPORARY_PASSWORD);
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(passwordEncoder.encode(TEMPORARY_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        doThrow(new RuntimeException("smtp down"))
                .when(emailService).sendUserCreatedWithTemporaryPassword(any(), any(), any());

        assertThatThrownBy(() -> usersManagementService.createUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("smtp down");
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

    private CreateUserRequest buildCreateUserRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Juan");
        request.setLastName("Pérez");
        request.setUsername("juanp");
        request.setEmail("juan@email.com");
        return request;
    }
}
