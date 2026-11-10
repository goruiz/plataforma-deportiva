package com.platform.backend.modules.users.presentation.responses.AccountsByMonthResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The accounts created in one month. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountsByMonthResponse {
    /** The month as {@code yyyy-MM}. */
    private String month;
    private long accounts;
}
