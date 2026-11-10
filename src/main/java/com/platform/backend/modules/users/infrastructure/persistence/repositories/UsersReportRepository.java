package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.irepositories.IUsersReportRepository;
import com.platform.backend.modules.users.domain.models.AccountsByMonth;
import com.platform.backend.modules.users.domain.models.AccountsByOrganization;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UsersReportRepository implements IUsersReportRepository {

    private final UsersReportJpaRepository usersReportJpaRepository;

    @Override
    public long countAccountsWithVerifiedEmail() {
        return usersReportJpaRepository.countAccountsWithVerifiedEmail();
    }

    @Override
    public List<AccountsByMonth> countAccountsCreatedSinceByMonth(LocalDateTime since) {
        return usersReportJpaRepository.countAccountsCreatedSinceByMonth(since).stream()
                .map(row -> new AccountsByMonth(
                        YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()), ((Number) row[2]).longValue()))
                .toList();
    }

    @Override
    public List<AccountsByOrganization> countAccountsByOrganization() {
        return usersReportJpaRepository.countAccountsByOrganization().stream()
                .map(row -> new AccountsByOrganization((UUID) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    public long countPlayersWithTeam() {
        return usersReportJpaRepository.countPlayersWithTeam();
    }

    @Override
    public long countPlayersWithoutTeam() {
        return usersReportJpaRepository.countPlayersWithoutTeam();
    }
}
