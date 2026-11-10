package com.platform.backend.modules.users.domain.models;

import java.time.YearMonth;

/** How many accounts were created in one calendar month. */
public record AccountsByMonth(YearMonth month, long accounts) {
}
