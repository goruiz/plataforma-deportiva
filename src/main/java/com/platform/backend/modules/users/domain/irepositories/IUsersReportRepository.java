package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.models.AccountsByMonth;
import com.platform.backend.modules.users.domain.models.AccountsByOrganization;

import java.time.LocalDateTime;
import java.util.List;

/** Totals of the accounts and players for the users report. Every read goes through the organization filter. */
public interface IUsersReportRepository {

    /** How many active accounts have confirmed their email. */
    long countAccountsWithVerifiedEmail();

    /** The accounts created at or after {@code since}, by calendar month; a month without any has no line. */
    List<AccountsByMonth> countAccountsCreatedSinceByMonth(LocalDateTime since);

    /** The active accounts of each organization (and of the platform owner, with no organization). */
    List<AccountsByOrganization> countAccountsByOrganization();

    /** How many players play for at least one team the caller can see. */
    long countPlayersWithTeam();

    /** How many players play for no team at all. Players are global, so only the platform owner has a use for this. */
    long countPlayersWithoutTeam();
}
