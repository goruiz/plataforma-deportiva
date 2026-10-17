package com.platform.backend.modules.holidays.application.iservices;

import com.platform.backend.modules.holidays.presentation.requests.CreateHolidayRequest.CreateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.requests.UpdateHolidayRequest.UpdateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.responses.HolidayResponse.HolidayResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IHolidaysService {

    List<HolidayResponse> getAll();

    HolidayResponse getById(UUID id);

    /** Refused when another holiday already covers the same day. */
    HolidayResponse create(CreateHolidayRequest request);

    HolidayResponse update(UUID id, UpdateHolidayRequest request);

    /** Soft delete. Matches already scheduled on that day are not moved. */
    void delete(UUID id);

    /** Every day within {@code [from, to]} (both included) on which no match may be scheduled, recurring holidays included. */
    Set<LocalDate> getNonPlayDatesBetween(LocalDate from, LocalDate to);
}
