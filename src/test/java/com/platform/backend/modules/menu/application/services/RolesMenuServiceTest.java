package com.platform.backend.modules.menu.application.services;

import com.platform.backend.modules.menu.application.mappers.RolesMenuMapper;
import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest.CreateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.SaveRoleMenusRequest.SaveRoleMenusRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest.UpdateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolesMenuServiceTest {

    @Mock private IRolesMenuRepository rolesMenuRepository;
    @Mock private IRoleRepository roleRepository;
    @Mock private IMenuRepository menuRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;
    @Spy private RolesMenuMapper rolesMenuMapper = Mappers.getMapper(RolesMenuMapper.class);

    @InjectMocks
    private RolesMenuService rolesMenuService;

    private RolesEntity adminRole;
    private RolesEntity ownerRole;
    private MenuEntity events;
    private MenuEntity teams;
    private MenuEntity roleMenusScreen;

    /** What the database holds for the role being saved; saves add to it like the real repository would. */
    private final List<RolesMenuEntity> storedAssignments = new ArrayList<>();

    @BeforeEach
    void setUp() {
        adminRole = role();
        ownerRole = role();
        events = menu(false);
        teams = menu(false);
        roleMenusScreen = menu(true);

        lenient().when(platformOwnerRole.isOwnerRole(ownerRole.getId())).thenReturn(true);
        lenient().when(roleRepository.findActiveById(adminRole.getId())).thenReturn(Optional.of(adminRole));
        lenient().when(roleRepository.findActiveById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
        lenient().when(menuRepository.findAllActive()).thenReturn(List.of(events, teams, roleMenusScreen));
        lenient().when(rolesMenuRepository.findActiveByRoleId(any())).thenAnswer(invocation -> List.copyOf(storedAssignments));
        lenient().when(rolesMenuRepository.save(any())).thenAnswer(invocation -> {
            RolesMenuEntity saved = invocation.getArgument(0);
            if (!storedAssignments.contains(saved)) {
                storedAssignments.add(saved);
            }
            return saved;
        });
    }

    private static RolesEntity role() {
        RolesEntity role = new RolesEntity();
        role.setId(UUID.randomUUID());
        return role;
    }

    private static MenuEntity menu(boolean ownerOnly) {
        MenuEntity menu = new MenuEntity();
        menu.setId(UUID.randomUUID());
        menu.setOwnerOnly(ownerOnly);
        return menu;
    }

    private RolesMenuEntity assignment(RolesEntity role, MenuEntity menu, boolean visible) {
        RolesMenuEntity assignment = new RolesMenuEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setRole(role);
        assignment.setMenu(menu);
        assignment.setVisible(visible);
        storedAssignments.add(assignment);
        return assignment;
    }

    private static SaveRoleMenusRequest menusOf(MenuEntity... menus) {
        SaveRoleMenusRequest request = new SaveRoleMenusRequest();
        request.setMenuIds(java.util.Arrays.stream(menus).map(MenuEntity::getId).toList());
        return request;
    }

    // ─── saveForRole ────────────────────────────────────────────────────────────

    @Test
    void saveForRole_whenSomeMenusAreHiddenAndOthersShown_thenTheRoleEndsSeeingExactlyTheListedOnes() {
        RolesMenuEntity eventsAssignment = assignment(adminRole, events, true);
        RolesMenuEntity teamsAssignment = assignment(adminRole, teams, false);

        List<RolesMenuResponse> result = rolesMenuService.saveForRole(adminRole.getId(), menusOf(teams));

        assertThat(eventsAssignment.getVisible()).isFalse();
        assertThat(teamsAssignment.getVisible()).isTrue();
        assertThat(result).extracting(RolesMenuResponse::getMenuId).containsExactlyInAnyOrder(events.getId(), teams.getId());
    }

    @Test
    void saveForRole_whenAMenuHasNoAssignmentYet_thenItIsCreatedVisible() {
        List<RolesMenuResponse> result = rolesMenuService.saveForRole(adminRole.getId(), menusOf(events));

        ArgumentCaptor<RolesMenuEntity> created = ArgumentCaptor.forClass(RolesMenuEntity.class);
        verify(rolesMenuRepository).save(created.capture());
        assertThat(created.getValue().getRole()).isSameAs(adminRole);
        assertThat(created.getValue().getMenu()).isSameAs(events);
        assertThat(created.getValue().getVisible()).isTrue();
        assertThat(result).extracting(RolesMenuResponse::getMenuId).containsExactly(events.getId());
    }

    @Test
    void saveForRole_whenNothingChanges_thenNothingIsSaved() {
        assignment(adminRole, events, true);
        assignment(adminRole, teams, false);

        rolesMenuService.saveForRole(adminRole.getId(), menusOf(events));

        verify(rolesMenuRepository, never()).save(any());
    }

    @Test
    void saveForRole_whenTheListIsEmpty_thenEveryMenuIsHiddenFromTheRole() {
        RolesMenuEntity eventsAssignment = assignment(adminRole, events, true);

        rolesMenuService.saveForRole(adminRole.getId(), menusOf());

        assertThat(eventsAssignment.getVisible()).isFalse();
    }

    @Test
    void saveForRole_whenAMenuDoesNotExist_thenNotFoundWithoutSavingAnything() {
        SaveRoleMenusRequest request = new SaveRoleMenusRequest();
        request.setMenuIds(List.of(events.getId(), UUID.randomUUID()));

        assertThatThrownBy(() -> rolesMenuService.saveForRole(adminRole.getId(), request))
                .isInstanceOf(EntityNotFoundException.class);

        verify(rolesMenuRepository, never()).save(any());
    }

    @Test
    void saveForRole_whenTheRoleDoesNotExist_thenNotFound() {
        UUID unknownRoleId = UUID.randomUUID();
        when(roleRepository.findActiveById(unknownRoleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolesMenuService.saveForRole(unknownRoleId, menusOf(events)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void saveForRole_whenAnotherRoleAsksForAnOwnerOnlyScreen_thenItIsRefused() {
        assertThatThrownBy(() -> rolesMenuService.saveForRole(adminRole.getId(), menusOf(events, roleMenusScreen)))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("menuIds", "OWNER_ONLY_MENU")));

        verify(rolesMenuRepository, never()).save(any());
    }

    @Test
    void saveForRole_whenTheOwnerRoleLeavesItsOwnerOnlyScreenOut_thenItIsKeptAnyway() {
        RolesMenuEntity screenAssignment = assignment(ownerRole, roleMenusScreen, true);

        List<RolesMenuResponse> result = rolesMenuService.saveForRole(ownerRole.getId(), menusOf(events));

        assertThat(screenAssignment.getVisible()).isTrue();
        assertThat(result).extracting(RolesMenuResponse::getMenuId).containsExactlyInAnyOrder(roleMenusScreen.getId(), events.getId());
    }

    @Test
    void saveForRole_whenTheOwnerRoleHadNoAssignmentForItsScreen_thenItIsCreated() {
        List<RolesMenuResponse> result = rolesMenuService.saveForRole(ownerRole.getId(), menusOf());

        assertThat(result).extracting(RolesMenuResponse::getMenuId).containsExactly(roleMenusScreen.getId());
    }

    // ─── create and update keep the same rule ───────────────────────────────────

    private CreateRolesMenuRequest grant(RolesEntity role, MenuEntity menu, boolean visible) {
        CreateRolesMenuRequest request = new CreateRolesMenuRequest();
        request.setRoleId(role.getId());
        request.setMenuId(menu.getId());
        request.setVisible(visible);
        when(menuRepository.findActiveById(menu.getId())).thenReturn(Optional.of(menu));
        return request;
    }

    @Test
    void create_whenAnotherRoleIsGivenAnOwnerOnlyScreen_thenItIsRefused() {
        CreateRolesMenuRequest request = grant(adminRole, roleMenusScreen, true);

        assertThatThrownBy(() -> rolesMenuService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("visible", "OWNER_ONLY_MENU")));

        verify(rolesMenuRepository, never()).save(any());
    }

    @Test
    void create_whenAnotherRoleIsGivenAnOwnerOnlyScreenHidden_thenItIsAllowed() {
        rolesMenuService.create(grant(adminRole, roleMenusScreen, false));

        verify(rolesMenuRepository, atLeastOnce()).save(any(RolesMenuEntity.class));
    }

    @Test
    void create_whenTheOwnerRoleIsGivenItsScreenHidden_thenItIsRefused() {
        CreateRolesMenuRequest request = grant(ownerRole, roleMenusScreen, false);

        assertThatThrownBy(() -> rolesMenuService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("visible", "OWNER_MENU_REQUIRED")));
    }

    @Test
    void update_whenTheOwnerRoleHidesItsScreen_thenItIsRefused() {
        RolesMenuEntity screenAssignment = assignment(ownerRole, roleMenusScreen, true);
        when(rolesMenuRepository.findActiveById(screenAssignment.getId())).thenReturn(Optional.of(screenAssignment));
        UpdateRolesMenuRequest request = new UpdateRolesMenuRequest();
        request.setVisible(false);

        assertThatThrownBy(() -> rolesMenuService.update(screenAssignment.getId(), request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("visible", "OWNER_MENU_REQUIRED")));

        assertThat(screenAssignment.getVisible()).isTrue();
    }

    @Test
    void update_whenAnOrdinaryScreenIsHiddenFromAnyRole_thenItWorks() {
        RolesMenuEntity eventsAssignment = assignment(adminRole, events, true);
        when(rolesMenuRepository.findActiveById(eventsAssignment.getId())).thenReturn(Optional.of(eventsAssignment));
        UpdateRolesMenuRequest request = new UpdateRolesMenuRequest();
        request.setVisible(false);

        rolesMenuService.update(eventsAssignment.getId(), request);

        assertThat(eventsAssignment.getVisible()).isFalse();
    }

    @Test
    void update_whenOnlyAPermissionFlagChanges_thenTheOwnerOnlyRuleDoesNotApply() {
        RolesMenuEntity screenAssignment = assignment(ownerRole, roleMenusScreen, true);
        when(rolesMenuRepository.findActiveById(screenAssignment.getId())).thenReturn(Optional.of(screenAssignment));
        UpdateRolesMenuRequest request = new UpdateRolesMenuRequest();
        request.setCanDelete(false);

        rolesMenuService.update(screenAssignment.getId(), request);

        assertThat(screenAssignment.getCanDelete()).isFalse();
    }

    @Test
    void delete_whenItIsTheOwnerRolesOwnerOnlyScreen_thenItIsRefused() {
        RolesMenuEntity screenAssignment = assignment(ownerRole, roleMenusScreen, true);
        when(rolesMenuRepository.findActiveById(screenAssignment.getId())).thenReturn(Optional.of(screenAssignment));

        assertThatThrownBy(() -> rolesMenuService.delete(screenAssignment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OWNER_MENU_REQUIRED");

        assertThat(screenAssignment.getDeletedAt()).isNull();
    }

    @Test
    void delete_whenItIsAnOrdinaryAssignment_thenItIsSoftDeleted() {
        RolesMenuEntity eventsAssignment = assignment(adminRole, events, true);
        when(rolesMenuRepository.findActiveById(eventsAssignment.getId())).thenReturn(Optional.of(eventsAssignment));

        rolesMenuService.delete(eventsAssignment.getId());

        assertThat(eventsAssignment.getDeletedAt()).isNotNull();
    }
}
