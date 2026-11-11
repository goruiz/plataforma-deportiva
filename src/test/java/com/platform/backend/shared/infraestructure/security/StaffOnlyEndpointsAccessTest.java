package com.platform.backend.shared.infraestructure.security;

import com.platform.backend.modules.courts.presentation.controllers.CourtsController;
import com.platform.backend.modules.events.presentation.controllers.CategoriesController;
import com.platform.backend.modules.events.presentation.controllers.EventsCategoriesController;
import com.platform.backend.modules.holidays.presentation.controllers.HolidaysController;
import com.platform.backend.modules.players.presentation.controllers.PlayersController;
import com.platform.backend.modules.sportcomplexes.presentation.controllers.SportComplexesController;
import com.platform.backend.modules.users.presentation.controllers.UsersManagementController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Creating, changing and deleting the records an organizer manages (accounts, players, courts, complexes, categories
 * and holidays) is for administrators only; a user of the app only reads them. The rule is an annotation that nothing
 * else exercises, so it is pinned here per endpoint: every write of these controllers must carry it, except the few
 * things a player does for themselves, which are listed by name so that opening a new one is a conscious decision.
 */
class StaffOnlyEndpointsAccessTest {

    /** Endpoints a player of the app calls for themselves: registering with a link, joining with a code, leaving a team. */
    private static final Set<String> SELF_SERVICE_WRITES = Set.of("register", "joinByCode", "removeFromTeam");

    /** Reads that list personal data of every person and so belong to the organizer, not to the app. */
    private static final Set<String> STAFF_ONLY_READS = Set.of("getAll", "search", "getUserById");

    static Stream<Arguments> writeEndpoints() {
        return Stream.of(
                        UsersManagementController.class,
                        PlayersController.class,
                        CourtsController.class,
                        SportComplexesController.class,
                        CategoriesController.class,
                        EventsCategoriesController.class,
                        HolidaysController.class)
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods())
                        .filter(StaffOnlyEndpointsAccessTest::isWrite)
                        .map(method -> Arguments.of(controller.getSimpleName(), method.getName(), method)));
    }

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("writeEndpoints")
    void everyWriteIsForAdministrators_unlessAPlayerDoesItForThemselves(String controller, String name, Method method) {
        PreAuthorize rule = method.getAnnotation(PreAuthorize.class);

        if (SELF_SERVICE_WRITES.contains(name)) {
            assertThat(rule).as("%s.%s is something a player does for themselves", controller, name).isNull();
        } else {
            assertThat(rule).as("%s.%s must be restricted", controller, name).isNotNull();
            assertThat(rule.value()).isIn(AccessRules.ADMIN_ONLY, AccessRules.PLATFORM_OWNER_ONLY);
        }
    }

    static Stream<Arguments> staffOnlyReads() {
        return Stream.of(PlayersController.class, UsersManagementController.class)
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(GetMapping.class)
                                && STAFF_ONLY_READS.contains(method.getName()))
                        .map(method -> Arguments.of(controller.getSimpleName(), method.getName(), method)));
    }

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("staffOnlyReads")
    void listingOrLookingUpPeopleIsForAdministrators(String controller, String name, Method method) {
        PreAuthorize rule = method.getAnnotation(PreAuthorize.class);

        assertThat(rule).as("%s.%s exposes personal data", controller, name).isNotNull();
        assertThat(rule.value()).isIn(AccessRules.ADMIN_ONLY, AccessRules.PLATFORM_OWNER_ONLY);
    }

    private static boolean isWrite(Method method) {
        return Stream.of(PostMapping.class, PutMapping.class, PatchMapping.class, DeleteMapping.class)
                .anyMatch(method::isAnnotationPresent);
    }

    /** Guards the lists above against a typo: a name that matches no endpoint would silently pin nothing. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("namedEndpoints")
    void everyNamedEndpointExists(String name) {
        List<String> declared = Stream.of(PlayersController.class, UsersManagementController.class)
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .map(Method::getName)
                .toList();

        assertThat(declared).contains(name);
    }

    static Stream<String> namedEndpoints() {
        return Stream.concat(SELF_SERVICE_WRITES.stream(), STAFF_ONLY_READS.stream());
    }
}
