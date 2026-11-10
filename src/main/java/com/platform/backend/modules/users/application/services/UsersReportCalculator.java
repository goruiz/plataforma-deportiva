package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.domain.models.AccountsByMonth;
import com.platform.backend.modules.users.presentation.responses.AccountsByMonthResponse.AccountsByMonthResponse;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** The arithmetic of the users report, apart from where the numbers are read. */
final class UsersReportCalculator {

    private UsersReportCalculator() {}

    /**
     * The {@code months} calendar months that end at {@code lastMonth}, oldest first, each with its accounts: the
     * database only answers for the months that have any, and a chart needs the quiet ones too.
     */
    static List<AccountsByMonthResponse> fillMonths(List<AccountsByMonth> found, YearMonth lastMonth, int months) {
        Map<YearMonth, Long> accountsByMonth = new HashMap<>();
        found.forEach(line -> accountsByMonth.merge(line.month(), line.accounts(), Long::sum));
        return IntStream.rangeClosed(1 - months, 0)
                .mapToObj(offset -> lastMonth.plusMonths(offset))
                .map(month -> new AccountsByMonthResponse(month.toString(), accountsByMonth.getOrDefault(month, 0L)))
                .toList();
    }
}
