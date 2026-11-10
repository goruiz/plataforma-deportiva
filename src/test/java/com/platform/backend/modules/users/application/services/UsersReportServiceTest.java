package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.users.application.iservices.IUsersManagementService;
import com.platform.backend.modules.users.domain.irepositories.IUsersReportRepository;
import com.platform.backend.modules.users.domain.models.AccountsByMonth;
import com.platform.backend.modules.users.domain.models.AccountsByOrganization;
import com.platform.backend.modules.users.presentation.responses.AccountsByMonthResponse.AccountsByMonthResponse;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UsersReportResponse.UsersReportResponse;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersReportServiceTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Mock private IUsersReportRepository usersReportRepository;
    @Mock private IUsersManagementService usersManagementService;
    @Mock private IOrganizationRepository organizationRepository;

    @InjectMocks
    private UsersReportService usersReportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usersReportService, "historyMonths", 3);
        lenient().when(usersManagementService.countUsersBySection(List.of())).thenReturn(new UserCountsResponse(1, 4, 15));
        lenient().when(usersReportRepository.countAccountsWithVerifiedEmail()).thenReturn(17L);
        lenient().when(usersReportRepository.countAccountsCreatedSinceByMonth(any())).thenReturn(List.of());
        lenient().when(usersReportRepository.countPlayersWithTeam()).thenReturn(9L);
        lenient().when(usersReportRepository.countPlayersWithoutTeam()).thenReturn(6L);
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static OrganizationsEntity organization(UUID id, String name) {
        OrganizationsEntity organization = new OrganizationsEntity();
        organization.setId(id);
        organization.setName(name);
        return organization;
    }

    @Test
    void theSectionsAndTheVerifiedEmailsAreAddedUp() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        UsersReportResponse report = usersReportService.buildReport(NOVEMBER);

        assertThat(report.getOwners()).isEqualTo(1);
        assertThat(report.getAdmins()).isEqualTo(4);
        assertThat(report.getAppUsers()).isEqualTo(15);
        assertThat(report.getVerifiedEmails()).isEqualTo(17);
        assertThat(report.getUnverifiedEmails()).isEqualTo(3);
        assertThat(report.getPlayersWithTeam()).isEqualTo(9);
    }

    @Test
    void theSeriesStartsAtTheFirstDayOfTheOldestMonthAndFillsTheQuietOnes() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        when(usersReportRepository.countAccountsCreatedSinceByMonth(LocalDateTime.of(2026, 9, 1, 0, 0)))
                .thenReturn(List.of(new AccountsByMonth(YearMonth.of(2026, 9), 5), new AccountsByMonth(NOVEMBER, 2)));

        List<AccountsByMonthResponse> series = usersReportService.buildReport(NOVEMBER).getAccountsByMonth();

        assertThat(series).extracting(AccountsByMonthResponse::getMonth).containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(series).extracting(AccountsByMonthResponse::getAccounts).containsExactly(5L, 0L, 2L);
    }

    @Test
    void anAdministratorOfAnOrganizationGetsNeitherTheOrganizationsNorThePlayersWithoutATeam() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        UsersReportResponse report = usersReportService.buildReport(NOVEMBER);

        assertThat(report.getAccountsByOrganization()).isEmpty();
        assertThat(report.getPlayersWithoutTeam()).isNull();
        verify(usersReportRepository, never()).countAccountsByOrganization();
        verify(usersReportRepository, never()).countPlayersWithoutTeam();
    }

    @Test
    void thePlatformOwnerGetsTheOrganizationsMostNumerousFirstAndTheirOwnAccountsUnnamed() {
        TenantTestSupport.actAsPlatformOwner();
        UUID liga = UUID.randomUUID();
        UUID estadio = UUID.randomUUID();
        when(organizationRepository.findAllActive())
                .thenReturn(List.of(organization(liga, "Liga Norte"), organization(estadio, "Estadio Sur")));
        when(usersReportRepository.countAccountsByOrganization()).thenReturn(List.of(
                new AccountsByOrganization(estadio, 4), new AccountsByOrganization(null, 1), new AccountsByOrganization(liga, 12)));

        UsersReportResponse report = usersReportService.buildReport(NOVEMBER);

        assertThat(report.getAccountsByOrganization()).extracting("organizationName", "accounts")
                .containsExactly(tuple("Liga Norte", 12L), tuple("Estadio Sur", 4L), tuple(null, 1L));
        assertThat(report.getPlayersWithoutTeam()).isEqualTo(6L);
    }
}
