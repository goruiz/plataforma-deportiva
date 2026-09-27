package com.platform.backend.modules.users.infrastructure.seed;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IRoleRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seeds a fixed admin account and a fixed regular-user account on startup, so the app
 * has usable credentials without depending on Flyway (disabled in every profile today)
 * or a separate manual data-loading step. Runs on every startup but is idempotent —
 * it checks before inserting, so restarts never create duplicates.
 * <p>
 * Excluded from "prd" so a hardcoded, source-controlled admin password is never
 * auto-created against a real production database.
 */
@Component
@Profile("!prd")
@RequiredArgsConstructor
public class UsersSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsersSeedRunner.class);

    private static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final String ADMIN_EMAIL = "admin@algo.com";
    private static final String ADMIN_PASSWORD = "admin@algo.com1A";
    private static final String REGULAR_EMAIL = "usuario@algo.com";
    private static final String REGULAR_PASSWORD = "usuario@algo.com1A";

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(adminRoleId)) {
            log.warn("app.security.admin-role-id is not configured; skipping admin/regular user seed.");
            return;
        }

        UUID adminRoleUuid = UUID.fromString(adminRoleId);
        ensureAdminRoleExists(adminRoleUuid);

        UsersEntity adminUser = ensureUserExists(ADMIN_EMAIL, "Administrador", "Principal", ADMIN_PASSWORD);
        ensureUserExists(REGULAR_EMAIL, "Usuario", "Demo", REGULAR_PASSWORD);

        ensureUserHasAdminRole(adminUser, adminRoleUuid);
    }

    /**
     * The admin role's id must match app.security.admin-role-id exactly, but Hibernate's
     * UuidGenerator always overwrites any id assigned before save() regardless of the
     * value already on the entity — so this one row is inserted with plain SQL instead
     * of going through the JPA repository, to guarantee the literal configured id is used.
     */
    private void ensureAdminRoleExists(UUID adminRoleUuid) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE id = ?", Integer.class, adminRoleUuid
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO roles (id, name, created_at) VALUES (?, ?, ?)",
                adminRoleUuid, ADMIN_ROLE_NAME, LocalDateTime.now()
        );
        log.info("Seeded '{}' role with id {}", ADMIN_ROLE_NAME, adminRoleUuid);
    }

    private UsersEntity ensureUserExists(String email, String firstName, String lastName, String rawPassword) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            UsersEntity user = new UsersEntity();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            UsersEntity saved = userRepository.save(user);
            log.info("Seeded user '{}'", email);
            return saved;
        });
    }

    private void ensureUserHasAdminRole(UsersEntity adminUser, UUID adminRoleUuid) {
        if (usersRolesRepository.existsActiveByUserAndRole(adminUser.getId(), adminRoleUuid)) {
            return;
        }

        RolesEntity adminRole = roleRepository.findById(adminRoleUuid)
                .orElseThrow(() -> new IllegalStateException(
                        "Admin role " + adminRoleUuid + " was not seeded correctly."
                ));

        UsersRolesEntity link = new UsersRolesEntity();
        link.setUser(adminUser);
        link.setRole(adminRole);
        usersRolesRepository.save(link);
        log.info("Granted '{}' role to seeded user '{}'", ADMIN_ROLE_NAME, adminUser.getEmail());
    }
}
