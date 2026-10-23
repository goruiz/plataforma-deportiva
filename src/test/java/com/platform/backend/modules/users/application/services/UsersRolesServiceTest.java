package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.mappers.UsersRolesMapper;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.CreateUsersRolesRequest.CreateUsersRolesRequest;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The owner role is what tells the system's seller apart from its customers: only the seller may hand it out. */
@ExtendWith(MockitoExtension.class)
class UsersRolesServiceTest {

    @Mock private IUsersRolesRepository usersRolesRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IRoleRepository roleRepository;
    @Mock private UsersRolesMapper usersRolesMapper;
    @Mock private PlatformOwnerRole platformOwnerRole;

    @InjectMocks
    private UsersRolesService usersRolesService;

    private UsersEntity user;
    private RolesEntity ordinaryRole;
    private RolesEntity ownerRole;

    @BeforeEach
    void setUp() {
        user = new UsersEntity();
        user.setId(UUID.randomUUID());
        ordinaryRole = role();
        ownerRole = role();

        lenient().when(platformOwnerRole.isOwnerRole(ownerRole.getId())).thenReturn(true);
        lenient().when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        lenient().when(roleRepository.findActiveById(ordinaryRole.getId())).thenReturn(Optional.of(ordinaryRole));
        lenient().when(roleRepository.findActiveById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
        lenient().when(usersRolesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static RolesEntity role() {
        RolesEntity role = new RolesEntity();
        role.setId(UUID.randomUUID());
        return role;
    }

    private CreateUsersRolesRequest assign(RolesEntity role) {
        CreateUsersRolesRequest request = new CreateUsersRolesRequest();
        request.setUserId(user.getId());
        request.setRoleId(role.getId());
        return request;
    }

    private UsersRolesEntity assignmentOf(RolesEntity role) {
        UsersRolesEntity assignment = new UsersRolesEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setUser(user);
        assignment.setRole(role);
        when(usersRolesRepository.findActiveById(assignment.getId())).thenReturn(Optional.of(assignment));
        return assignment;
    }

    @Test
    void create_whenAnOrganizationAdministratorAssignsTheOwnerRole_thenItIsDenied() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatThrownBy(() -> usersRolesService.create(assign(ownerRole))).isInstanceOf(AccessDeniedException.class);

        verify(usersRolesRepository, never()).save(any());
    }

    @Test
    void create_whenThePlatformOwnerAssignsTheOwnerRole_thenItIsSaved() {
        TenantTestSupport.actAsPlatformOwner();

        usersRolesService.create(assign(ownerRole));

        verify(usersRolesRepository).save(any(UsersRolesEntity.class));
    }

    @Test
    void create_whenAnOrganizationAdministratorAssignsAnOrdinaryRole_thenItIsSaved() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        usersRolesService.create(assign(ordinaryRole));

        verify(usersRolesRepository).save(any(UsersRolesEntity.class));
    }

    @Test
    void delete_whenAnOrganizationAdministratorRemovesTheOwnerRole_thenItIsDenied() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        UsersRolesEntity assignment = assignmentOf(ownerRole);

        assertThatThrownBy(() -> usersRolesService.delete(assignment.getId())).isInstanceOf(AccessDeniedException.class);

        assertThat(assignment.getDeletedAt()).isNull();
    }

    @Test
    void delete_whenAnOrganizationAdministratorRemovesAnOrdinaryRole_thenItIsSoftDeleted() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        UsersRolesEntity assignment = assignmentOf(ordinaryRole);

        usersRolesService.delete(assignment.getId());

        assertThat(assignment.getDeletedAt()).isNotNull();
    }
}
