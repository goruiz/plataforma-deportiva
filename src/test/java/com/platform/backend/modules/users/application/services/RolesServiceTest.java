package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.mappers.RoleMapper;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.responses.RoleResponse.RoleResponse;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolesServiceTest {

    @Mock private IRoleRepository roleRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;
    @Spy private RoleMapper roleMapper = Mappers.getMapper(RoleMapper.class);

    @InjectMocks
    private RolesService rolesService;

    private RolesEntity adminRole;
    private RolesEntity ownerRole;

    @BeforeEach
    void setUp() {
        adminRole = role("ADMIN");
        ownerRole = role("PLATFORM_OWNER");
        lenient().when(platformOwnerRole.isOwnerRole(ownerRole.getId())).thenReturn(true);
        lenient().when(roleRepository.findAllActive()).thenReturn(List.of(adminRole, ownerRole));
        lenient().when(roleRepository.findActiveById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static RolesEntity role(String name) {
        RolesEntity role = new RolesEntity();
        role.setId(UUID.randomUUID());
        role.setName(name);
        return role;
    }

    @Test
    void getAll_whenAnOrganizationAdministratorAsks_thenTheOwnerRoleIsNotListed() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThat(rolesService.getAll()).extracting(RoleResponse::getName).containsExactly("ADMIN");
    }

    @Test
    void getAll_whenThePlatformOwnerAsks_thenEveryRoleIsListed() {
        TenantTestSupport.actAsPlatformOwner();

        assertThat(rolesService.getAll()).extracting(RoleResponse::getName).containsExactly("ADMIN", "PLATFORM_OWNER");
    }

    @Test
    void getAll_whenThePlatformOwnerAsks_thenOnlyTheOwnerRoleIsMarkedAsTheOwners() {
        TenantTestSupport.actAsPlatformOwner();

        assertThat(rolesService.getAll()).extracting(RoleResponse::getName, RoleResponse::isPlatformOwner)
                .containsExactly(tuple("ADMIN", false), tuple("PLATFORM_OWNER", true));
    }

    @Test
    void getById_whenAnOrganizationAdministratorAsksForTheOwnerRole_thenItIsNotFound() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatThrownBy(() -> rolesService.getById(ownerRole.getId())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getById_whenThePlatformOwnerAsksForTheOwnerRole_thenItIsReturned() {
        TenantTestSupport.actAsPlatformOwner();

        assertThat(rolesService.getById(ownerRole.getId()).getName()).isEqualTo("PLATFORM_OWNER");
    }
}
