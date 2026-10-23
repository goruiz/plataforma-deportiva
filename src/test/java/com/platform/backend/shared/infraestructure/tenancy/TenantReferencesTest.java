package com.platform.backend.shared.infraestructure.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantReferencesTest {

    private static final UUID SOME_ID = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    @Test
    void requireVisible_whenTheCallerIsRestrictedAndCannotSeeTheId_thenItIsRefused() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatThrownBy(() -> TenantReferences.requireVisible(
                SOME_ID, false, id -> false, () -> new IllegalStateException("hidden")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("hidden");
    }

    @Test
    void requireVisible_whenTheCallerIsRestrictedAndSeesTheId_thenItPasses() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatCode(() -> TenantReferences.requireVisible(
                SOME_ID, true, id -> true, () -> new IllegalStateException("hidden")))
                .doesNotThrowAnyException();
    }

    @Test
    void requireVisible_whenTheIdIsMissingAndRequired_thenItIsRefusedWithoutLookingItUp() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatThrownBy(() -> TenantReferences.requireVisible(
                null, true, id -> { throw new AssertionError("must not look up a missing id"); },
                () -> new IllegalStateException("required")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireVisible_whenTheIdIsMissingAndOptional_thenItPasses() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThatCode(() -> TenantReferences.requireVisible(
                null, false, id -> false, () -> new IllegalStateException("hidden")))
                .doesNotThrowAnyException();
    }

    @Test
    void requireVisible_whenTheCallerSeesEverything_thenNothingIsChecked() {
        TenantTestSupport.actAsPlatformOwner();

        assertThatCode(() -> TenantReferences.requireVisible(
                SOME_ID, true, id -> false, () -> new IllegalStateException("hidden")))
                .doesNotThrowAnyException();
        assertThatCode(() -> TenantReferences.requireVisible(
                null, true, id -> false, () -> new IllegalStateException("hidden")))
                .doesNotThrowAnyException();
    }
}
