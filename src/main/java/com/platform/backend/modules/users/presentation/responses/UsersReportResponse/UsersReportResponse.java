package com.platform.backend.modules.users.presentation.responses.UsersReportResponse;

import com.platform.backend.modules.users.presentation.responses.AccountsByMonthResponse.AccountsByMonthResponse;
import com.platform.backend.modules.users.presentation.responses.AccountsByOrganizationResponse.AccountsByOrganizationResponse;
import lombok.Data;

import java.util.List;

/**
 * The accounts and players the caller can see, added up: the platform owner reads the whole platform, an administrator
 * only their organization.
 */
@Data
public class UsersReportResponse {

    /** The accounts holding the platform owner's role; always zero for an organization's administrator. */
    private long owners;
    private long admins;
    /** Accounts holding neither role: the ones created from the mobile app. */
    private long appUsers;
    /**
     * How many of the accounts above an administrator suspended (they are still counted in the figures above: the
     * report shows them apart instead of hiding them). Same split by type, and the total of the three.
     */
    private long suspendedOwners;
    private long suspendedAdmins;
    private long suspendedAppUsers;
    private long suspendedAccounts;
    private long verifiedEmails;
    private long unverifiedEmails;
    /** The last months in order, the current one last; a month without new accounts is there with zero. */
    private List<AccountsByMonthResponse> accountsByMonth;
    /** Only for the platform owner (empty for an administrator), the most numerous organization first. */
    private List<AccountsByOrganizationResponse> accountsByOrganization;
    /** Players who play for at least one team the caller can see. */
    private long playersWithTeam;
    /** Players with no team at all; only for the platform owner, because players are global (null for anyone else). */
    private Long playersWithoutTeam;
}
