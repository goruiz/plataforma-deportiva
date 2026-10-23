package com.platform.backend.shared.infraestructure.tenancy;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationOwnerListenerTest {

    private static final UUID OWN_ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID OTHER_ORGANIZATION_ID = UUID.randomUUID();

    private final OrganizationOwnerListener listener = new OrganizationOwnerListener();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    @Test
    void assignOrganization_whenAnOrganizationAdministratorCreates_thenTheRecordIsTheirs() {
        TenantTestSupport.actAsOrganizationAdmin(OWN_ORGANIZATION_ID);
        TeamsEntity team = new TeamsEntity();

        listener.assignOrganization(team);

        assertThat(team.getOrganizationId()).isEqualTo(OWN_ORGANIZATION_ID);
    }

    @Test
    void assignOrganization_whenAnOrganizationAdministratorTriesAnotherOrganization_thenItIsOverridden() {
        TenantTestSupport.actAsOrganizationAdmin(OWN_ORGANIZATION_ID);
        TeamsEntity team = new TeamsEntity();
        team.setOrganizationId(OTHER_ORGANIZATION_ID);

        listener.assignOrganization(team);

        assertThat(team.getOrganizationId()).isEqualTo(OWN_ORGANIZATION_ID);
    }

    @Test
    void assignOrganization_whenThePlatformOwnerCreatesWithoutChoosing_thenTheRecordHasNoOrganization() {
        TenantTestSupport.actAsPlatformOwner();
        TeamsEntity team = new TeamsEntity();

        listener.assignOrganization(team);

        assertThat(team.getOrganizationId()).isNull();
    }

    @Test
    void assignOrganization_whenThePlatformOwnerChoseAnOrganization_thenItIsKept() {
        TenantTestSupport.actAsPlatformOwner();
        TeamsEntity team = new TeamsEntity();
        team.setOrganizationId(OTHER_ORGANIZATION_ID);

        listener.assignOrganization(team);

        assertThat(team.getOrganizationId()).isEqualTo(OTHER_ORGANIZATION_ID);
    }

    @Test
    void assignOrganization_whenTheEntityDoesNotBelongToAnOrganization_thenNothingHappens() {
        TenantTestSupport.actAsOrganizationAdmin(OWN_ORGANIZATION_ID);

        listener.assignOrganization(new Object());
    }
}
