package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.domain.models.AccountsByMonth;
import com.platform.backend.modules.users.presentation.responses.AccountsByMonthResponse.AccountsByMonthResponse;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsersReportCalculatorTest {

    @Test
    void fillMonths_listsEveryMonthOldestFirstWithZerosForTheQuietOnes() {
        List<AccountsByMonthResponse> series = UsersReportCalculator.fillMonths(
                List.of(new AccountsByMonth(YearMonth.of(2026, 1), 3), new AccountsByMonth(YearMonth.of(2026, 3), 1)),
                YearMonth.of(2026, 3), 3);

        assertThat(series).extracting(AccountsByMonthResponse::getMonth).containsExactly("2026-01", "2026-02", "2026-03");
        assertThat(series).extracting(AccountsByMonthResponse::getAccounts).containsExactly(3L, 0L, 1L);
    }

    @Test
    void fillMonths_crossesTheTurnOfTheYear() {
        List<AccountsByMonthResponse> series = UsersReportCalculator.fillMonths(List.of(), YearMonth.of(2026, 2), 4);

        assertThat(series).extracting(AccountsByMonthResponse::getMonth)
                .containsExactly("2025-11", "2025-12", "2026-01", "2026-02");
    }

    @Test
    void fillMonths_ignoresAMonthOutsideTheWindow() {
        List<AccountsByMonthResponse> series = UsersReportCalculator.fillMonths(
                List.of(new AccountsByMonth(YearMonth.of(2020, 5), 99)), YearMonth.of(2026, 3), 2);

        assertThat(series).extracting(AccountsByMonthResponse::getAccounts).containsExactly(0L, 0L);
    }

    @Test
    void fillMonths_addsTwoLinesOfTheSameMonth() {
        List<AccountsByMonthResponse> series = UsersReportCalculator.fillMonths(
                List.of(new AccountsByMonth(YearMonth.of(2026, 3), 2), new AccountsByMonth(YearMonth.of(2026, 3), 5)),
                YearMonth.of(2026, 3), 1);

        assertThat(series.get(0).getAccounts()).isEqualTo(7);
    }
}
