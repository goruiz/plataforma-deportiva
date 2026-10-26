package com.platform.backend.shared.infraestructure.security;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class CreatorAccessTest {

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID SOMEONE_ELSE_ID = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static EventsEntity eventCreatedBy(UUID creatorId) {
        EventsEntity event = new EventsEntity();
        event.setCreatedBy(creatorId);
        return event;
    }

    @Test
    void requireAdminOrCreator_whenNobodyIsAuthenticated_thenNothingIsRestricted() {
        // Scheduled jobs and public endpoints run without a user.
        assertThatCode(() -> CreatorAccess.requireAdminOrCreator(SOMEONE_ELSE_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireAdminOrCreator_whenTheCallerCreatedTheRecord_thenAllowed() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");

        assertThatCode(() -> CreatorAccess.requireAdminOrCreator(CALLER_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireAdminOrCreator_whenAnotherAccountCreatedTheRecord_thenForbiddenWithTheCode() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");

        assertThatThrownBy(() -> CreatorAccess.requireAdminOrCreator(SOMEONE_ELSE_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessError.NOT_THE_CREATOR.name());
    }

    @Test
    void requireAdminOrCreator_whenTheRecordHasNoCreator_thenOnlyAnAdministratorMayChangeIt() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");

        assertThatThrownBy(() -> CreatorAccess.requireAdminOrCreator((UUID) null))
                .isInstanceOf(AccessDeniedException.class);

        TenantTestSupport.actAsOrganizationAdmin(CALLER_ID, UUID.randomUUID());
        assertThatCode(() -> CreatorAccess.requireAdminOrCreator((UUID) null)).doesNotThrowAnyException();
    }

    @Test
    void requireAdminOrCreator_whenTheCallerIsAnAdministrator_thenAnyRecordIsAllowed() {
        TenantTestSupport.actAsOrganizationAdmin(CALLER_ID, UUID.randomUUID());

        assertThatCode(() -> CreatorAccess.requireAdminOrCreator(SOMEONE_ELSE_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireAdminOrCreator_whenTheCallerIsThePlatformOwner_thenAnyRecordIsAllowed() {
        TenantTestSupport.actAsPlatformOwner();

        assertThatCode(() -> CreatorAccess.requireAdminOrCreator(SOMEONE_ELSE_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireAdminOrCreator_byId_whenTheCallerCreatedTheReferencedRecord_thenAllowed() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");
        UUID eventId = UUID.randomUUID();

        assertThatCode(() -> CreatorAccess.requireAdminOrCreator(eventId, id -> Optional.of(eventCreatedBy(CALLER_ID))))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAdminOrCreator_byId_whenAnotherAccountCreatedTheReferencedRecord_thenForbidden() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");

        assertThatThrownBy(() -> CreatorAccess.requireAdminOrCreator(
                UUID.randomUUID(), id -> Optional.of(eventCreatedBy(SOMEONE_ELSE_ID))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAdminOrCreator_byId_whenTheReferencedRecordDoesNotExistOrThereIsNoId_thenForbiddenForAMobileAccount() {
        TenantTestSupport.actAsMobileAccount(CALLER_ID, "juan@email.com");

        assertThatThrownBy(() -> CreatorAccess.requireAdminOrCreator(UUID.randomUUID(), id -> Optional.empty()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> CreatorAccess.requireAdminOrCreator(null, id -> Optional.of(eventCreatedBy(CALLER_ID))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAdminOrCreator_byId_whenTheCallerIsAnAdministratorOrNobody_thenTheRecordIsNotEvenLookedUp() {
        AtomicBoolean lookedUp = new AtomicBoolean(false);

        TenantTestSupport.actAsOrganizationAdmin(CALLER_ID, UUID.randomUUID());
        CreatorAccess.requireAdminOrCreator(UUID.randomUUID(), id -> {
            lookedUp.set(true);
            return Optional.empty();
        });
        TenantTestSupport.actAsNobody();
        CreatorAccess.requireAdminOrCreator(UUID.randomUUID(), id -> {
            lookedUp.set(true);
            return Optional.empty();
        });

        assertThat(lookedUp).isFalse();
    }
}
