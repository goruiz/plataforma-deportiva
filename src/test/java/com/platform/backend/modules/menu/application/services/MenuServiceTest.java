package com.platform.backend.modules.menu.application.services;

import com.platform.backend.modules.menu.application.mappers.MenuMapper;
import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.exception.MenuError;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.ReorderMenusRequest.ReorderMenusRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.platform.backend.modules.menu.MenuTestFixtures.group;
import static com.platform.backend.modules.menu.MenuTestFixtures.screen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    private static final int MAX_DEPTH = 3;

    @Mock private IMenuRepository menuRepository;
    @Mock private IRolesMenuRepository rolesMenuRepository;
    @Mock private IUsersRolesRepository usersRolesRepository;
    @Mock private IRoleRepository roleRepository;
    @Mock private PlatformOwnerRole platformOwnerRole;

    private MenuService menuService;

    private RolesEntity ownerRole;
    private RolesEntity adminRole;

    private MenuEntity dashboard;
    private MenuEntity competition;
    private MenuEntity events;
    private MenuEntity formats;
    private MenuEntity platform;
    private MenuEntity menusScreen;

    /** What the database holds; saves add to it the way the real repository would. */
    private final List<MenuEntity> storedMenus = new ArrayList<>();
    private final List<RolesMenuEntity> storedAssignments = new ArrayList<>();

    /**
     * Dashboard
     * Competition ── Events, Formats
     * Platform (owner only) ── Menus (owner only, system)
     */
    @BeforeEach
    void setUp() {
        ownerRole = role();
        adminRole = role();

        dashboard = screen("Dashboard", "/dashboard", null, 0);
        competition = group("Competition", null, 1);
        events = screen("Events", "/events", competition, 0);
        formats = screen("Formats", "/formats", competition, 1);
        platform = group("Platform", null, 2);
        platform.setOwnerOnly(true);
        menusScreen = screen("Menus", "/menus", platform, 0);
        menusScreen.setOwnerOnly(true);
        menusScreen.setSystem(true);
        storedMenus.addAll(List.of(dashboard, competition, events, formats, platform, menusScreen));

        lenient().when(platformOwnerRole.id()).thenReturn(Optional.of(ownerRole.getId()));
        lenient().when(platformOwnerRole.isOwnerRole(ownerRole.getId())).thenReturn(true);
        lenient().when(roleRepository.findActiveById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
        // Like the real query: by order, then by name.
        lenient().when(menuRepository.findAllActive()).thenAnswer(invocation -> storedMenus.stream()
                .sorted(Comparator.comparing(MenuEntity::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MenuEntity::getName))
                .toList());
        lenient().when(menuRepository.findActiveById(any())).thenAnswer(invocation -> storedMenus.stream()
                .filter(menu -> menu.getId().equals(invocation.getArgument(0))).findFirst());
        lenient().when(menuRepository.save(any())).thenAnswer(invocation -> {
            MenuEntity saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            if (!storedMenus.contains(saved)) {
                storedMenus.add(saved);
            }
            return saved;
        });
        lenient().when(rolesMenuRepository.findActiveByRoleId(any())).thenAnswer(invocation -> storedAssignments.stream()
                .filter(assignment -> assignment.getRole().getId().equals(invocation.getArgument(0))).toList());
        lenient().when(rolesMenuRepository.findActiveByMenuIds(any())).thenAnswer(invocation -> {
            java.util.Collection<UUID> menuIds = invocation.getArgument(0);
            return storedAssignments.stream().filter(assignment -> menuIds.contains(assignment.getMenu().getId())).toList();
        });
        lenient().when(rolesMenuRepository.save(any())).thenAnswer(invocation -> {
            RolesMenuEntity saved = invocation.getArgument(0);
            if (!storedAssignments.contains(saved)) {
                storedAssignments.add(saved);
            }
            return saved;
        });

        menuService = new MenuService(menuRepository, rolesMenuRepository, usersRolesRepository, roleRepository,
                platformOwnerRole, Mappers.getMapper(MenuMapper.class), MAX_DEPTH);
    }

    private static RolesEntity role() {
        RolesEntity role = new RolesEntity();
        role.setId(UUID.randomUUID());
        return role;
    }

    private RolesMenuEntity assignment(RolesEntity role, MenuEntity menu, boolean visible) {
        RolesMenuEntity assignment = RolesMenuEntity.visibleFor(role, menu);
        assignment.setId(UUID.randomUUID());
        assignment.setVisible(visible);
        storedAssignments.add(assignment);
        return assignment;
    }

    private static CreateMenuRequest createRequest(String name, String url, MenuEntity parent) {
        CreateMenuRequest request = new CreateMenuRequest();
        request.setName(name);
        request.setUrl(url);
        request.setIdParentMenu(parent == null ? null : parent.getId());
        return request;
    }

    private static ReorderMenusRequest reorderRequest(MenuEntity parent, MenuEntity... menus) {
        ReorderMenusRequest request = new ReorderMenusRequest();
        request.setIdParentMenu(parent == null ? null : parent.getId());
        request.setMenuIds(java.util.Arrays.stream(menus).map(MenuEntity::getId).toList());
        return request;
    }

    private static void assertRejectedWith(Runnable action, String field, MenuError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(FieldValidationException.class,
                        exception -> assertThat(exception.getFieldErrors()).containsExactly(entry(field, error.name())));
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenItIsValid_thenItGoesAtTheEndOfItsParentAndIsActive() {
        MenuResponse created = menuService.create(createRequest("  Reports  ", "/reports", competition));

        assertThat(created.getName()).isEqualTo("Reports");
        assertThat(created.getUrl()).isEqualTo("/reports");
        assertThat(created.getIdParentMenu()).isEqualTo(competition.getId());
        assertThat(created.getOrder()).isEqualTo(2);
        assertThat(created.getIsActive()).isTrue();
        assertThat(created.isOwnerOnly()).isFalse();
    }

    @Test
    void create_whenItOpensAScreen_thenTheOwnerRoleSeesItRightAway() {
        menuService.create(createRequest("Reports", "/reports", null));

        assertThat(storedAssignments).singleElement().satisfies(assignment -> {
            assertThat(assignment.getRole()).isSameAs(ownerRole);
            assertThat(assignment.getVisible()).isTrue();
            assertThat(assignment.getMenu().getName()).isEqualTo("Reports");
        });
    }

    @Test
    void create_whenItIsAnOrdinaryGroup_thenNobodyIsGrantedIt() {
        menuService.create(createRequest("Reports", null, null));

        assertThat(storedAssignments).isEmpty();
    }

    @Test
    void create_whenItGoesInsideAnOwnerOnlyGroup_thenItIsOwnerOnlyToo() {
        MenuResponse created = menuService.create(createRequest("Audit", "/audit", platform));

        assertThat(created.isOwnerOnly()).isTrue();
    }

    @Test
    void create_whenTheNameIsBlank_thenItIsRejected() {
        assertRejectedWith(() -> menuService.create(createRequest("   ", "/x", null)), "name", MenuError.NAME_REQUIRED);
    }

    @Test
    void create_whenTheNameExistsInAnyCase_thenItIsRejected() {
        assertRejectedWith(() -> menuService.create(createRequest(" events", "/x", null)), "name", MenuError.NAME_ALREADY_EXISTS);
    }

    @Test
    void create_whenTheUrlIsNotAPathInsideTheApp_thenItIsRejected() {
        for (String badUrl : List.of("reports", "//example.com", "/with space", "https://example.com")) {
            assertRejectedWith(() -> menuService.create(createRequest("Reports", badUrl, null)), "url", MenuError.INVALID_URL);
        }
    }

    @Test
    void create_whenAnotherItemOpensTheSameUrl_thenItIsRejected() {
        assertRejectedWith(() -> menuService.create(createRequest("Reports", "/EVENTS", null)), "url", MenuError.URL_ALREADY_IN_USE);
    }

    @Test
    void create_whenTheParentDoesNotExist_thenItIsRejected() {
        CreateMenuRequest request = createRequest("Reports", "/reports", null);
        request.setIdParentMenu(UUID.randomUUID());

        assertRejectedWith(() -> menuService.create(request), "idParentMenu", MenuError.PARENT_NOT_FOUND);
    }

    @Test
    void create_whenTheParentOpensAScreen_thenItIsRejected() {
        assertRejectedWith(() -> menuService.create(createRequest("Reports", "/reports", events)), "idParentMenu", MenuError.PARENT_HAS_URL);
    }

    @Test
    void create_whenItWouldGoDeeperThanTheLimit_thenItIsRejected() {
        MenuEntity second = group("Second", competition, 5);
        MenuEntity third = group("Third", second, 0);
        storedMenus.addAll(List.of(second, third));

        assertRejectedWith(() -> menuService.create(createRequest("Reports", "/reports", third)), "idParentMenu", MenuError.MAX_DEPTH_EXCEEDED);
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenOnlyTheNameIsSent_thenNothingElseChanges() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setName(" Matches ");

        MenuResponse updated = menuService.update(events.getId(), request);

        assertThat(updated.getName()).isEqualTo("Matches");
        assertThat(updated.getUrl()).isEqualTo("/events");
        assertThat(updated.getIdParentMenu()).isEqualTo(competition.getId());
        assertThat(updated.getOrder()).isZero();
    }

    @Test
    void update_whenTheTextsAreSentBlank_thenTheyAreCleared() {
        events.setDescription("Old");
        events.setIcon("cilStar");
        events.setTranslationKey("menu.admin.events");
        events.setDescriptionTranslationKey("menu.admin.events_description");
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setDescription("  ");
        request.setIcon("");
        request.setTranslationKey("");
        request.setDescriptionTranslationKey("");

        MenuResponse updated = menuService.update(events.getId(), request);

        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getIcon()).isNull();
        assertThat(updated.getTranslationKey()).isNull();
        assertThat(updated.getDescriptionTranslationKey()).isNull();
    }

    @Test
    void update_whenTheItemKeepsItsOwnNameAndUrl_thenThatIsNotACollision() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setName("events");
        request.setUrl("/events");

        MenuResponse updated = menuService.update(events.getId(), request);

        assertThat(updated.getName()).isEqualTo("events");
    }

    @Test
    void update_whenAGroupWithItemsGetsAUrl_thenItIsRejected() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setUrl("/competition");

        assertRejectedWith(() -> menuService.update(competition.getId(), request), "url", MenuError.URL_NOT_ALLOWED_IN_GROUP);
    }

    @Test
    void update_whenASystemItemLosesItsUrl_thenItIsRejected() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setUrl("/elsewhere");

        assertRejectedWith(() -> menuService.update(menusScreen.getId(), request), "url", MenuError.SYSTEM_MENU);
    }

    @Test
    void update_whenASystemItemOrTheGroupHoldingItIsHidden_thenItIsRejected() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setIsActive(false);

        assertRejectedWith(() -> menuService.update(menusScreen.getId(), request), "isActive", MenuError.SYSTEM_MENU);
        assertRejectedWith(() -> menuService.update(platform.getId(), request), "isActive", MenuError.SYSTEM_MENU);
    }

    @Test
    void update_whenAnOrdinaryItemIsHidden_thenItIsHiddenAndNotDeleted() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setIsActive(false);

        MenuResponse updated = menuService.update(formats.getId(), request);

        assertThat(updated.getIsActive()).isFalse();
        assertThat(formats.getDeletedAt()).isNull();
    }

    @Test
    void update_whenAnItemMovesToAnotherGroup_thenItGoesAtTheEndOfIt() {
        MenuEntity facilities = group("Facilities", null, 3);
        MenuEntity courts = screen("Courts", "/courts", facilities, 0);
        storedMenus.addAll(List.of(facilities, courts));
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setIdParentMenu(facilities.getId());

        MenuResponse updated = menuService.update(events.getId(), request);

        assertThat(updated.getIdParentMenu()).isEqualTo(facilities.getId());
        assertThat(updated.getOrder()).isEqualTo(1);
    }

    @Test
    void update_whenAnItemGoesToTheTopLevel_thenItLeavesItsParent() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setTopLevel(true);

        MenuResponse updated = menuService.update(events.getId(), request);

        assertThat(updated.getIdParentMenu()).isNull();
        assertThat(updated.getOrder()).isEqualTo(3);
    }

    @Test
    void update_whenItAsksForBothAParentAndTheTopLevel_thenItIsRejected() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setTopLevel(true);
        request.setIdParentMenu(competition.getId());

        assertRejectedWith(() -> menuService.update(events.getId(), request), "idParentMenu", MenuError.PARENT_CONFLICT);
    }

    @Test
    void update_whenAGroupWouldEndUpInsideItsOwnSubmenu_thenItIsRejected() {
        MenuEntity inner = group("Inner", competition, 5);
        storedMenus.add(inner);
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setIdParentMenu(inner.getId());

        assertRejectedWith(() -> menuService.update(competition.getId(), request), "idParentMenu", MenuError.PARENT_CREATES_LOOP);
        request.setIdParentMenu(competition.getId());
        assertRejectedWith(() -> menuService.update(competition.getId(), request), "idParentMenu", MenuError.PARENT_CREATES_LOOP);
    }

    @Test
    void update_whenAGroupWithItsContentWouldGoPastTheLimit_thenItIsRejected() {
        // Platform (2 levels with its screen) inside Competition (1) would be 3 levels: fine. Inside a deeper group it is not.
        MenuEntity deeper = group("Deeper", competition, 5);
        storedMenus.add(deeper);
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setIdParentMenu(deeper.getId());
        platform.setOwnerOnly(false);

        assertRejectedWith(() -> menuService.update(platform.getId(), request), "idParentMenu", MenuError.MAX_DEPTH_EXCEEDED);
    }

    @Test
    void update_whenAGroupBecomesOwnerOnly_thenEverythingInsideIsRestrictedToTheOwner() {
        RolesMenuEntity adminSeesEvents = assignment(adminRole, events, true);
        RolesMenuEntity adminSeesFormats = assignment(adminRole, formats, true);
        RolesMenuEntity ownerSeesEvents = assignment(ownerRole, events, false);
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setOwnerOnly(true);

        MenuResponse updated = menuService.update(competition.getId(), request);

        assertThat(updated.isOwnerOnly()).isTrue();
        assertThat(events.isOwnerOnly()).isTrue();
        assertThat(formats.isOwnerOnly()).isTrue();
        assertThat(adminSeesEvents.getVisible()).isFalse();
        assertThat(adminSeesFormats.getVisible()).isFalse();
        assertThat(ownerSeesEvents.getVisible()).isTrue();
        assertThat(storedAssignments).filteredOn(assignment -> assignment.getRole() == ownerRole)
                .extracting(assignment -> assignment.getMenu().getId())
                .contains(competition.getId(), events.getId(), formats.getId());
    }

    @Test
    void update_whenAnItemMovesIntoAnOwnerOnlyGroup_thenItBecomesOwnerOnly() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setIdParentMenu(platform.getId());

        MenuResponse updated = menuService.update(events.getId(), request);

        assertThat(updated.isOwnerOnly()).isTrue();
    }

    @Test
    void update_whenAnItemInsideAnOwnerOnlyGroupIsOpenedToEveryone_thenItIsRejected() {
        MenuEntity audit = screen("Audit", "/audit", platform, 5);
        audit.setOwnerOnly(true);
        storedMenus.add(audit);
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setOwnerOnly(false);

        assertRejectedWith(() -> menuService.update(audit.getId(), request), "ownerOnly", MenuError.PARENT_OWNER_ONLY);
    }

    @Test
    void update_whenASystemItemIsOpenedToEveryone_thenItIsRejected() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setOwnerOnly(false);

        assertRejectedWith(() -> menuService.update(menusScreen.getId(), request), "ownerOnly", MenuError.SYSTEM_MENU);
    }

    @Test
    void update_whenTheItemDoesNotExist_thenItIsNotFound() {
        assertThatThrownBy(() -> menuService.update(UUID.randomUUID(), new UpdateMenuRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── reorder ────────────────────────────────────────────────────────────────

    @Test
    void reorder_whenTheSiblingsAreListedInANewOrder_thenTheyAreNumberedInThatOrder() {
        List<MenuTreeResponse> tree = menuService.reorder(reorderRequest(competition, formats, events));

        assertThat(formats.getOrder()).isZero();
        assertThat(events.getOrder()).isEqualTo(1);
        MenuTreeResponse competitionNode = tree.stream().filter(node -> node.getId().equals(competition.getId())).findFirst().orElseThrow();
        assertThat(competitionNode.getSubmenus()).extracting(MenuTreeResponse::getId).containsExactly(formats.getId(), events.getId());
    }

    @Test
    void reorder_whenOnlySomeSiblingsAreListed_thenTheOthersFollowInTheirOwnOrder() {
        menuService.reorder(reorderRequest(null, platform));

        assertThat(platform.getOrder()).isZero();
        assertThat(dashboard.getOrder()).isEqualTo(1);
        assertThat(competition.getOrder()).isEqualTo(2);
    }

    @Test
    void reorder_whenAnItemComesFromAnotherParent_thenItMovesInAtTheListedPlace() {
        MenuEntity facilities = group("Facilities", null, 3);
        MenuEntity courts = screen("Courts", "/courts", facilities, 0);
        storedMenus.addAll(List.of(facilities, courts));

        menuService.reorder(reorderRequest(facilities, events, courts));

        assertThat(events.getIdParentMenu()).isEqualTo(facilities.getId());
        assertThat(events.getOrder()).isZero();
        assertThat(courts.getOrder()).isEqualTo(1);
    }

    @Test
    void reorder_whenTheListRepeatsAnItem_thenItIsRejected() {
        assertRejectedWith(() -> menuService.reorder(reorderRequest(competition, events, events)), "menuIds", MenuError.DUPLICATED_MENUS);
    }

    @Test
    void reorder_whenAnItemDoesNotExist_thenItIsNotFound() {
        ReorderMenusRequest request = reorderRequest(competition, events);
        request.setMenuIds(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> menuService.reorder(request)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void reorder_whenAGroupIsPlacedInsideItself_thenItIsRejected() {
        assertRejectedWith(() -> menuService.reorder(reorderRequest(competition, competition)), "idParentMenu", MenuError.PARENT_CREATES_LOOP);
    }

    @Test
    void reorder_whenTheParentOpensAScreen_thenItIsRejected() {
        assertRejectedWith(() -> menuService.reorder(reorderRequest(events, formats)), "idParentMenu", MenuError.PARENT_HAS_URL);
    }

    @Test
    void reorder_whenItemsAreMovedIntoAnOwnerOnlyGroup_thenTheyBecomeOwnerOnlyAndLoseTheirOtherRoles() {
        RolesMenuEntity adminSeesEvents = assignment(adminRole, events, true);

        menuService.reorder(reorderRequest(platform, menusScreen, events));

        assertThat(events.isOwnerOnly()).isTrue();
        assertThat(adminSeesEvents.getVisible()).isFalse();
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenTheItemHoldsOtherItems_thenItIsRefused() {
        assertThatThrownBy(() -> menuService.delete(competition.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MenuError.HAS_SUBMENUS.name());
        assertThat(competition.getDeletedAt()).isNull();
    }

    @Test
    void delete_whenTheItemIsOneThePlatformNeeds_thenItIsRefused() {
        assertThatThrownBy(() -> menuService.delete(menusScreen.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MenuError.SYSTEM_MENU.name());
    }

    @Test
    void delete_whenTheItemIsFree_thenItAndWhatRolesHadOfItAreRemoved() {
        RolesMenuEntity adminSeesFormats = assignment(adminRole, formats, true);
        RolesMenuEntity adminSeesEvents = assignment(adminRole, events, true);

        menuService.delete(formats.getId());

        assertThat(formats.getDeletedAt()).isNotNull();
        assertThat(adminSeesFormats.getDeletedAt()).isNotNull();
        assertThat(adminSeesEvents.getDeletedAt()).isNull();
        assertThat(events.getDeletedAt()).isNull();
    }

    // ─── getForUser / getAll ────────────────────────────────────────────────────

    @Test
    void getForUser_whenARoleOnlyHoldsAScreen_thenItsGroupIsShownAroundIt() {
        UUID userId = UUID.randomUUID();
        lenient().when(usersRolesRepository.findActiveRoleIdsByUserId(userId)).thenReturn(List.of(adminRole.getId()));
        lenient().when(rolesMenuRepository.findActiveVisibleByRoleIds(any()))
                .thenReturn(List.of(assignment(adminRole, events, true), assignment(adminRole, dashboard, true)));

        List<MenuTreeResponse> menu = menuService.getForUser(userId);

        assertThat(menu).extracting(MenuTreeResponse::getName).containsExactly("Dashboard", "Competition");
        assertThat(menu.get(1).getSubmenus()).extracting(MenuTreeResponse::getName).containsExactly("Events");
    }

    @Test
    void getForUser_whenTheUserHasNoRole_thenTheMenuIsEmpty() {
        UUID userId = UUID.randomUUID();
        lenient().when(usersRolesRepository.findActiveRoleIdsByUserId(userId)).thenReturn(List.of());

        assertThat(menuService.getForUser(userId)).isEmpty();
    }

    @Test
    void getAll_showsHiddenItemsToo() {
        formats.setIsActive(false);

        List<MenuTreeResponse> all = menuService.getAll();

        MenuTreeResponse competitionNode = all.stream().filter(node -> node.getId().equals(competition.getId())).findFirst().orElseThrow();
        assertThat(competitionNode.getSubmenus()).extracting(MenuTreeResponse::getName).containsExactly("Events", "Formats");
    }
}
