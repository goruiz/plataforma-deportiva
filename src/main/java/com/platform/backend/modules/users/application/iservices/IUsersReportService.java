package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.responses.UsersReportResponse.UsersReportResponse;

public interface IUsersReportService {

    /** The accounts and players the caller can see, added up (see {@link UsersReportResponse}). */
    UsersReportResponse getReport();
}
