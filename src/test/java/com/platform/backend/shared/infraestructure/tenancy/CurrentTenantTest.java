package com.platform.backend.shared.infraestructure.tenancy;

import com.platform.backend.shared.domain.tenancy.TenantScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentTenantTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    @Test
    void scope_whenNobodyIsAuthenticated_thenNothingIsRestricted() {
        // Login, public endpoints and scheduled jobs run without a user.
        assertThat(CurrentTenant.scope().isUnrestricted()).isTrue();
    }

    @Test
    void scope_whenThePrincipalIsNotAnAuthenticatedUser_thenNothingIsRestricted() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThat(CurrentTenant.scope().isUnrestricted()).isTrue();
    }

    @Test
    void scope_whenAnOrganizationAdministratorIsCalling_thenItIsRestrictedToTheirOrganization() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);

        TenantScope scope = CurrentTenant.scope();

        assertThat(scope.restricted()).isTrue();
        assertThat(scope.organizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(scope.organizationIdForFilter()).isEqualTo(ORGANIZATION_ID);
    }

    @Test
    void scope_whenThePlatformOwnerIsCalling_thenNothingIsRestricted() {
        TenantTestSupport.actAsPlatformOwner();

        assertThat(CurrentTenant.scope().isUnrestricted()).isTrue();
        assertThat(CurrentTenant.isPlatformOwner()).isTrue();
    }

    @Test
    void scope_whenAPlayerWithATeamIsCalling_thenItIsRestrictedToTheirOrganizationAndSeesPublicEvents() {
        TenantTestSupport.actAsPlayerOfOrganization(ORGANIZATION_ID);

        TenantScope scope = CurrentTenant.scope();

        assertThat(scope.restricted()).isTrue();
        assertThat(scope.organizationIdForFilter()).isEqualTo(ORGANIZATION_ID);
        assertThat(scope.seesPublicEvents()).isTrue();
        assertThat(CurrentTenant.isPlatformOwner()).isFalse();
    }

    @Test
    void scope_whenAPlayerWithoutATeamIsCalling_thenTheyOnlySeePublicEvents() {
        TenantTestSupport.actAsMobileAccountWithoutOrganization();

        TenantScope scope = CurrentTenant.scope();

        assertThat(scope.restricted()).isTrue();
        assertThat(scope.organizationIdForFilter()).isEqualTo(TenantScope.NO_ORGANIZATION);
        assertThat(scope.seesPublicEvents()).isTrue();
    }

    @Test
    void scope_whenAnAdministratorIsCalling_thenOtherOrganizationsPublicEventsAreNotTheirs() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);

        assertThat(CurrentTenant.scope().seesPublicEvents()).isFalse();
    }

    @Test
    void scope_whenNothingIsRestricted_thenPublicEventsAreNotAFilterOfTheirOwn() {
        TenantTestSupport.actAsPlatformOwner();

        assertThat(CurrentTenant.scope().seesPublicEvents()).isFalse();
    }

    @Test
    void callerEmail_whenSomeoneIsCalling_thenItIsTheEmailOfTheirAccountInLowerCase() {
        TenantTestSupport.actAsPlayerOfOrganization(ORGANIZATION_ID);

        assertThat(CurrentTenant.callerEmail()).isEqualTo("juan@email.com");
    }

    @Test
    void callerEmail_whenNobodyIsCalling_thenItMatchesNoAccount() {
        assertThat(CurrentTenant.callerEmail()).isEqualTo(CurrentTenant.NO_EMAIL);
    }

    @Test
    void organizationIdForFilter_whenNothingIsRestricted_thenItIsAnIdThatMatchesNoOrganization() {
        assertThat(TenantScope.unrestricted().organizationIdForFilter()).isEqualTo(TenantScope.NO_ORGANIZATION);
    }

    @Test
    void resolvers_followTheCurrentCaller() {
        TenantTestSupport.actAsOrganizationAdmin(ORGANIZATION_ID);

        assertThat(new TenantUnrestrictedResolver().get()).isFalse();
        assertThat(new TenantOrganizationIdResolver().get()).isEqualTo(ORGANIZATION_ID);

        TenantTestSupport.actAsPlatformOwner();

        assertThat(new TenantUnrestrictedResolver().get()).isTrue();
        assertThat(new TenantOrganizationIdResolver().get()).isEqualTo(TenantScope.NO_ORGANIZATION);

        TenantTestSupport.actAsPlayerOfOrganization(ORGANIZATION_ID);

        assertThat(new TenantIncludesPublicEventsResolver().get()).isTrue();
        assertThat(new TenantCallerEmailResolver().get()).isEqualTo("juan@email.com");
    }
}
