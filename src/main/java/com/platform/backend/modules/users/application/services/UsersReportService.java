package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.entities.OrganizationsEntity;
import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.users.application.iservices.IUsersManagementService;
import com.platform.backend.modules.users.application.iservices.IUsersReportService;
import com.platform.backend.modules.users.domain.irepositories.IUsersReportRepository;
import com.platform.backend.modules.users.domain.models.AccountsByOrganization;
import com.platform.backend.modules.users.presentation.responses.AccountsByOrganizationResponse.AccountsByOrganizationResponse;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UsersReportResponse.UsersReportResponse;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The accounts and players of the platform added up for the report screen. What the caller sees is decided by the
 * organization filter, not here: an administrator's totals are their organization's. TODO: when accounts can be
 * suspended, count them here too.
 */
@Service
@RequiredArgsConstructor
public class UsersReportService implements IUsersReportService {

    private final IUsersReportRepository usersReportRepository;
    private final IUsersManagementService usersManagementService;
    private final IOrganizationRepository organizationRepository;

    /** How many calendar months the "accounts created" series covers, the current one included. */
    @Value("${app.reports.users-history-months:12}")
    private int historyMonths;

    @Override
    @Transactional(readOnly = true)
    public UsersReportResponse getReport() {
        return buildReport(YearMonth.now());
    }

    UsersReportResponse buildReport(YearMonth currentMonth) {
        // The three sections split every account (the owner role wins over the administrator one).
        UserCountsResponse accounts = usersManagementService.countUsersBySection(List.of());
        long totalAccounts = accounts.getOwners() + accounts.getAdmins() + accounts.getAppUsers();
        long verified = usersReportRepository.countAccountsWithVerifiedEmail();
        boolean platformOwner = CurrentTenant.isPlatformOwner();

        UsersReportResponse report = new UsersReportResponse();
        report.setOwners(accounts.getOwners());
        report.setAdmins(accounts.getAdmins());
        report.setAppUsers(accounts.getAppUsers());
        report.setVerifiedEmails(verified);
        report.setUnverifiedEmails(totalAccounts - verified);
        report.setAccountsByMonth(UsersReportCalculator.fillMonths(
                usersReportRepository.countAccountsCreatedSinceByMonth(
                        currentMonth.minusMonths(historyMonths - 1L).atDay(1).atStartOfDay()),
                currentMonth, historyMonths));
        report.setAccountsByOrganization(platformOwner ? accountsByOrganization() : List.of());
        report.setPlayersWithTeam(usersReportRepository.countPlayersWithTeam());
        report.setPlayersWithoutTeam(platformOwner ? usersReportRepository.countPlayersWithoutTeam() : null);
        return report;
    }

    /** The most numerous first; the platform owner's own accounts (no organization) keep their line, unnamed. */
    private List<AccountsByOrganizationResponse> accountsByOrganization() {
        Map<UUID, String> organizationNames = organizationRepository.findAllActive().stream()
                .collect(Collectors.toMap(OrganizationsEntity::getId, OrganizationsEntity::getName));
        return usersReportRepository.countAccountsByOrganization().stream()
                .sorted(Comparator.comparingLong(AccountsByOrganization::accounts).reversed())
                .map(line -> new AccountsByOrganizationResponse(
                        line.organizationId(), organizationNames.get(line.organizationId()), line.accounts()))
                .toList();
    }
}
