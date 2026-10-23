package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.enums.UserSection;
import com.platform.backend.modules.users.domain.filters.UserListFilter;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UserListItemResponse.UserListItemResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.modules.users.presentation.responses.UserRoleResponse.UserRoleResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.security.password.TemporaryPasswordGenerator;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
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
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class UsersManagementServiceTest {

    private static final UUID ADMIN_ROLE_ID = UUID.fromString("4fd36dd3-c40a-4063-9e88-fed84872564f");
    private static final UUID OWNER_ROLE_ID = UUID.fromString("0b1f7d52-6c0a-4a56-9f0e-3d0f4c8f2a11");
    private static final String TEMPORARY_PASSWORD = "Temp1234Abcd";
    private static final String ENCODED_PASSWORD = "encoded_temporary_password";

    @Mock private IUserRepository userRepository;
    @Mock private IUsersRolesRepository usersRolesRepository;
    @Mock private IOrganizationRepository organizationRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;
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

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    // ─── createUser: which organization the new account belongs to ──────────────

    private void givenUserCreationWillSucceed(CreateUserRequest request) {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn(TEMPORARY_PASSWORD);
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(passwordEncoder.encode(TEMPORARY_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userMapper.toUserResponse(userEntity)).thenReturn(userResponse);
    }

    @Test
    void createUser_whenAnOrganizationAdministratorCreates_thenTheAccountIsInTheirOrganizationWhateverTheRequestSays() {
        UUID ownOrganizationId = UUID.randomUUID();
        TenantTestSupport.actAsOrganizationAdmin(ownOrganizationId);
        CreateUserRequest request = buildCreateUserRequest();
        request.setOrganizationId(UUID.randomUUID());
        givenUserCreationWillSucceed(request);

        usersManagementService.createUser(request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(ownOrganizationId);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void createUser_whenThePlatformOwnerChoosesAnOrganization_thenTheAccountIsInIt() {
        TenantTestSupport.actAsPlatformOwner();
        UUID organizationId = UUID.randomUUID();
        CreateUserRequest request = buildCreateUserRequest();
        request.setOrganizationId(organizationId);
        givenUserCreationWillSucceed(request);
        when(organizationRepository.findActiveById(organizationId)).thenReturn(Optional.of(new OrganizationsEntity()));

        usersManagementService.createUser(request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void createUser_whenThePlatformOwnerChoosesNoOrganization_thenTheAccountBelongsToThePlatform() {
        TenantTestSupport.actAsPlatformOwner();
        CreateUserRequest request = buildCreateUserRequest();
        givenUserCreationWillSucceed(request);

        usersManagementService.createUser(request);

        assertThat(userEntity.getOrganizationId()).isNull();
    }

    @Test
    void createUser_whenThePlatformOwnerChoosesAnOrganizationThatDoesNotExist_thenNotFoundWithoutSaving() {
        TenantTestSupport.actAsPlatformOwner();
        UUID unknownOrganizationId = UUID.randomUUID();
        CreateUserRequest request = buildCreateUserRequest();
        request.setOrganizationId(unknownOrganizationId);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn(TEMPORARY_PASSWORD);
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(organizationRepository.findActiveById(unknownOrganizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersManagementService.createUser(request)).isInstanceOf(EntityNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
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

    // ─── updateUser: moving an account to another organization ──────────────────

    private UpdateUserRequest givenAnUpdateThatWillSucceed(UUID requestedOrganizationId) {
        UUID userId = UUID.randomUUID();
        userEntity.setId(userId);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setOrganizationId(requestedOrganizationId);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userMapper.toUserResponse(userEntity)).thenReturn(userResponse);
        return request;
    }

    @Test
    void updateUser_whenThePlatformOwnerChoosesAnotherOrganization_thenTheAccountMovesToIt() {
        TenantTestSupport.actAsPlatformOwner();
        UUID newOrganizationId = UUID.randomUUID();
        userEntity.setOrganizationId(UUID.randomUUID());
        UpdateUserRequest request = givenAnUpdateThatWillSucceed(newOrganizationId);
        when(organizationRepository.findActiveById(newOrganizationId)).thenReturn(Optional.of(new OrganizationsEntity()));

        usersManagementService.updateUser(userEntity.getId(), request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(newOrganizationId);
        verify(userRepository).save(userEntity);
    }

    @Test
    void updateUser_whenThePlatformOwnerMovesAPlatformAccountIntoAnOrganization_thenItBelongsToIt() {
        TenantTestSupport.actAsPlatformOwner();
        UUID organizationId = UUID.randomUUID();
        UpdateUserRequest request = givenAnUpdateThatWillSucceed(organizationId);
        when(organizationRepository.findActiveById(organizationId)).thenReturn(Optional.of(new OrganizationsEntity()));

        usersManagementService.updateUser(userEntity.getId(), request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void updateUser_whenTheOrganizationDoesNotExist_thenNotFoundWithoutSaving() {
        TenantTestSupport.actAsPlatformOwner();
        UUID oldOrganizationId = UUID.randomUUID();
        UUID unknownOrganizationId = UUID.randomUUID();
        userEntity.setOrganizationId(oldOrganizationId);
        UUID userId = UUID.randomUUID();
        userEntity.setId(userId);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setOrganizationId(unknownOrganizationId);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(userEntity));
        when(organizationRepository.findActiveById(unknownOrganizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersManagementService.updateUser(userId, request)).isInstanceOf(EntityNotFoundException.class);

        assertThat(userEntity.getOrganizationId()).isEqualTo(oldOrganizationId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_whenNoOrganizationIsSent_thenTheAccountStaysWhereItIs() {
        TenantTestSupport.actAsPlatformOwner();
        UUID organizationId = UUID.randomUUID();
        userEntity.setOrganizationId(organizationId);
        UpdateUserRequest request = givenAnUpdateThatWillSucceed(null);

        usersManagementService.updateUser(userEntity.getId(), request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(organizationId);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void updateUser_whenTheOrganizationSentIsTheCurrentOne_thenItIsNotLookedUpAgain() {
        TenantTestSupport.actAsPlatformOwner();
        UUID organizationId = UUID.randomUUID();
        userEntity.setOrganizationId(organizationId);
        UpdateUserRequest request = givenAnUpdateThatWillSucceed(organizationId);

        usersManagementService.updateUser(userEntity.getId(), request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(organizationId);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void updateUser_whenAnOrganizationAdministratorSendsAnotherOrganization_thenItIsIgnored() {
        UUID ownOrganizationId = UUID.randomUUID();
        TenantTestSupport.actAsOrganizationAdmin(ownOrganizationId);
        userEntity.setOrganizationId(ownOrganizationId);
        UpdateUserRequest request = givenAnUpdateThatWillSucceed(UUID.randomUUID());

        usersManagementService.updateUser(userEntity.getId(), request);

        assertThat(userEntity.getOrganizationId()).isEqualTo(ownOrganizationId);
        verifyNoInteractions(organizationRepository);
    }

    // ─── getUsers: the pages of each section ────────────────────────────────────

    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);
    private static final List<String> NO_SEARCH = List.of();

    private UsersEntity accountOfPage(UUID id, String email) {
        UsersEntity account = new UsersEntity();
        account.setId(id);
        account.setEmail(email);
        return account;
    }

    private ArgumentCaptor<UserListFilter> captureFilterOfPageQuery() {
        ArgumentCaptor<UserListFilter> filter = ArgumentCaptor.forClass(UserListFilter.class);
        when(userRepository.findActivePage(filter.capture(), eq(FIRST_PAGE)))
                .thenReturn(PageResult.empty(FIRST_PAGE));
        return filter;
    }

    @Test
    void getUsers_whenAskingForTheAdministrators_thenTheyHoldTheAdministratorRoleAndNotTheOwnersOne() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));
        ArgumentCaptor<UserListFilter> filter = captureFilterOfPageQuery();

        usersManagementService.getUsers(UserSection.ADMINS, FIRST_PAGE, List.of("ana"));

        assertThat(filter.getValue().withAnyRoleIds()).containsExactly(ADMIN_ROLE_ID);
        assertThat(filter.getValue().withoutAnyRoleIds()).containsExactly(OWNER_ROLE_ID);
        assertThat(filter.getValue().searchTerms()).containsExactly("ana");
    }

    @Test
    void getUsers_whenAskingForTheOwners_thenTheyHoldTheOwnerRole() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));
        ArgumentCaptor<UserListFilter> filter = captureFilterOfPageQuery();

        usersManagementService.getUsers(UserSection.OWNERS, FIRST_PAGE, NO_SEARCH);

        assertThat(filter.getValue().withAnyRoleIds()).containsExactly(OWNER_ROLE_ID);
        assertThat(filter.getValue().withoutAnyRoleIds()).isEmpty();
    }

    @Test
    void getUsers_whenAskingForTheAppUsers_thenTheyHoldNeitherStaffRole() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));
        ArgumentCaptor<UserListFilter> filter = captureFilterOfPageQuery();

        usersManagementService.getUsers(UserSection.APP_USERS, FIRST_PAGE, NO_SEARCH);

        assertThat(filter.getValue().withAnyRoleIds()).isEmpty();
        assertThat(filter.getValue().withoutAnyRoleIds()).containsExactlyInAnyOrder(ADMIN_ROLE_ID, OWNER_ROLE_ID);
    }

    @Test
    void getUsers_whenNoStaffRoleIsConfigured_thenTheAppUsersAreEveryone() {
        configureAdminRoleId("");
        when(platformOwnerRole.id()).thenReturn(Optional.empty());
        ArgumentCaptor<UserListFilter> filter = captureFilterOfPageQuery();

        usersManagementService.getUsers(UserSection.APP_USERS, FIRST_PAGE, NO_SEARCH);

        assertThat(filter.getValue().withAnyRoleIds()).isEmpty();
        assertThat(filter.getValue().withoutAnyRoleIds()).isEmpty();
    }

    @Test
    void getUsers_whenAskingForEveryAccount_thenNoRoleConditionIsApplied() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        ArgumentCaptor<UserListFilter> filter = captureFilterOfPageQuery();

        usersManagementService.getUsers(UserSection.ALL, FIRST_PAGE, NO_SEARCH);

        assertThat(filter.getValue().withAnyRoleIds()).isEmpty();
        assertThat(filter.getValue().withoutAnyRoleIds()).isEmpty();
    }

    @Test
    void getUsers_whenTheRoleOfTheSectionIsNotConfigured_thenThePageIsEmptyWithoutQueryingTheDatabase() {
        configureAdminRoleId("");
        when(platformOwnerRole.id()).thenReturn(Optional.empty());

        PageResponse<UserListItemResponse> owners = usersManagementService.getUsers(UserSection.OWNERS, FIRST_PAGE, NO_SEARCH);
        PageResponse<UserListItemResponse> administrators = usersManagementService.getUsers(UserSection.ADMINS, FIRST_PAGE, NO_SEARCH);

        assertThat(owners.getContent()).isEmpty();
        assertThat(owners.getTotalElements()).isZero();
        assertThat(administrators.getContent()).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void getUsers_whenThePageHasAccounts_thenTheirRolesAreReadTogetherInOneQuery() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        UUID anaId = UUID.randomUUID();
        UUID luisId = UUID.randomUUID();
        UsersEntity ana = accountOfPage(anaId, "ana@email.com");
        UsersEntity luis = accountOfPage(luisId, "luis@email.com");
        RolesEntity administrator = new RolesEntity();
        administrator.setId(ADMIN_ROLE_ID);
        administrator.setName("Administrador");
        PageQuery secondPage = new PageQuery(1, 2);
        when(userRepository.findActivePage(any(), eq(secondPage))).thenReturn(new PageResult<>(List.of(ana, luis), 5, secondPage));
        when(usersRolesRepository.findActiveRolesByUserIds(List.of(anaId, luisId))).thenReturn(Map.of(anaId, List.of(administrator)));
        when(userMapper.toUserListItemResponse(any(UsersEntity.class))).thenAnswer(call -> {
            UserListItemResponse item = new UserListItemResponse();
            item.setId(call.<UsersEntity>getArgument(0).getId());
            return item;
        });
        when(userMapper.toUserRoleResponse(administrator)).thenAnswer(call -> {
            UserRoleResponse role = new UserRoleResponse();
            role.setName("Administrador");
            return role;
        });

        PageResponse<UserListItemResponse> page = usersManagementService.getUsers(UserSection.ADMINS, secondPage, NO_SEARCH);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).extracting(UserListItemResponse::getId).containsExactly(anaId, luisId);
        assertThat(page.getContent().get(0).getRoles()).extracting(UserRoleResponse::getName).containsExactly("Administrador");
        assertThat(page.getContent().get(1).getRoles()).isEmpty();
        verify(usersRolesRepository).findActiveRolesByUserIds(List.of(anaId, luisId));
    }

    // ─── countUsersBySection ────────────────────────────────────────────────────

    @Test
    void countUsersBySection_whenAskedForTheTabs_thenTheAppUsersAreWhatIsLeftOfTheTotal() {
        configureAdminRoleId(ADMIN_ROLE_ID.toString());
        when(platformOwnerRole.id()).thenReturn(Optional.of(OWNER_ROLE_ID));
        when(userRepository.countActive(any())).thenAnswer(call -> {
            UserListFilter filter = call.getArgument(0);
            if (filter.withAnyRoleIds().contains(OWNER_ROLE_ID)) {
                return 2L;
            }
            if (filter.withAnyRoleIds().contains(ADMIN_ROLE_ID)) {
                return 30L;
            }
            return 1_000L;
        });

        UserCountsResponse counts = usersManagementService.countUsersBySection(NO_SEARCH);

        assertThat(counts.getOwners()).isEqualTo(2);
        assertThat(counts.getAdmins()).isEqualTo(30);
        assertThat(counts.getAppUsers()).isEqualTo(968);
    }

    @Test
    void countUsersBySection_whenNoStaffRoleIsConfigured_thenEveryoneIsAnAppUser() {
        configureAdminRoleId("");
        when(platformOwnerRole.id()).thenReturn(Optional.empty());
        when(userRepository.countActive(any())).thenReturn(40L);

        UserCountsResponse counts = usersManagementService.countUsersBySection(NO_SEARCH);

        assertThat(counts.getOwners()).isZero();
        assertThat(counts.getAdmins()).isZero();
        assertThat(counts.getAppUsers()).isEqualTo(40);
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
