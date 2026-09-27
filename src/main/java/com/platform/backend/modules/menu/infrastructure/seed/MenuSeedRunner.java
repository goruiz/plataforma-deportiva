package com.platform.backend.modules.menu.infrastructure.seed;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.entities.RolesMenuEntity;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.domain.irepositories.IRolesMenuRepository;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Seeds the admin panel's own top-level menu entries ("Usuarios", "Eventos") and grants
 * them to the fixed admin role in roles_menu, so GET /menu/me returns a non-empty sidebar
 * for administrators out of the box. Runs on every startup but is idempotent — checks
 * existence before inserting, so restarts never create duplicates.
 * <p>
 * Unlike UsersSeedRunner, this is not excluded from "prd": these are real navigation
 * entries the admin panel needs to function, not throwaway test credentials.
 */
@Component
@RequiredArgsConstructor
public class MenuSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuSeedRunner.class);

    private final IMenuRepository menuRepository;
    private final IRolesMenuRepository rolesMenuRepository;
    private final IRoleRepository roleRepository;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(adminRoleId)) {
            log.warn("app.security.admin-role-id is not configured; skipping admin menu seed.");
            return;
        }

        UUID adminRoleUuid = UUID.fromString(adminRoleId);
        RolesEntity adminRole = roleRepository.findById(adminRoleUuid).orElse(null);
        if (adminRole == null) {
            log.warn("Admin role {} does not exist yet; skipping admin menu seed.", adminRoleUuid);
            return;
        }

        MenuEntity usersMenu = ensureMenuExists(
                "Usuarios", "Gestión de usuarios del sistema",
                "menu.admin.users", "menu.admin.users_description",
                "cilPeople", "/users", (short) 1, 1
        );
        MenuEntity eventsMenu = ensureMenuExists(
                "Eventos", "Gestión de eventos deportivos",
                "menu.admin.events", "menu.admin.events_description",
                "cilCalendar", "/events", (short) 2, 2
        );

        grantVisibilityToAdmin(adminRole, usersMenu);
        grantVisibilityToAdmin(adminRole, eventsMenu);
    }

    private MenuEntity ensureMenuExists(String name, String description, String nameTranslationKey,
                                         String descriptionTranslationKey, String icon, String url,
                                         short navOrder, int order) {
        return menuRepository.findActiveByName(name).orElseGet(() -> {
            MenuEntity menu = new MenuEntity();
            menu.setName(name);
            menu.setDescription(description);
            menu.setTranslationKey(nameTranslationKey);
            menu.setDescriptionTranslationKey(descriptionTranslationKey);
            menu.setIcon(icon);
            menu.setUrl(url);
            menu.setNavOrder(navOrder);
            menu.setOrder(order);
            menu.setIsActive(true);
            MenuEntity saved = menuRepository.save(menu);
            log.info("Seeded admin menu item '{}'", name);
            return saved;
        });
    }

    private void grantVisibilityToAdmin(RolesEntity adminRole, MenuEntity menu) {
        if (rolesMenuRepository.existsActiveByRoleAndMenu(adminRole.getId(), menu.getId())) {
            return;
        }

        RolesMenuEntity grant = new RolesMenuEntity();
        grant.setRole(adminRole);
        grant.setMenu(menu);
        grant.setVisible(true);
        grant.setCanRead(true);
        grant.setCanCreat(true);
        grant.setCanUpdate(true);
        grant.setCanDelete(true);
        rolesMenuRepository.save(grant);
        log.info("Granted admin role visibility for menu item '{}'", menu.getName());
    }
}
