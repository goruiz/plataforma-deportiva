package com.platform.backend.modules.holidays.application.services;

import com.platform.backend.modules.holidays.application.iservices.IHolidaysService;
import com.platform.backend.modules.holidays.application.mappers.HolidayMapper;
import com.platform.backend.modules.holidays.domain.entities.HolidaysEntity;
import com.platform.backend.modules.holidays.domain.exception.HolidayError;
import com.platform.backend.modules.holidays.domain.irepositories.IHolidayRepository;
import com.platform.backend.modules.holidays.presentation.requests.CreateHolidayRequest.CreateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.requests.UpdateHolidayRequest.UpdateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.responses.HolidayResponse.HolidayResponse;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HolidaysService implements IHolidaysService {

    private static final String DATE_FIELD = "date";
    private static final String END_DATE_FIELD = "endDate";

    private final IHolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;

    @Transactional(readOnly = true)
    @Override
    public List<HolidayResponse> getAll() {
        return holidayRepository.findAllActive()
                .stream()
                .map(holidayMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public HolidayResponse getById(UUID id) {
        return holidayMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public HolidayResponse create(CreateHolidayRequest request) {
        validatePeriod(request.getDate(), request.getEndDate(), request.isRecurring(), null);

        HolidaysEntity holiday = holidayMapper.toEntity(request);
        holiday.setName(request.getName().trim());
        return holidayMapper.toResponse(holidayRepository.save(holiday));
    }

    @Transactional
    @Override
    public HolidayResponse update(UUID id, UpdateHolidayRequest request) {
        HolidaysEntity holiday = findActiveOrThrow(id);

        keepDurationWhenMoved(request, holiday);

        // The checks need the final values, so what the request leaves out is taken from the stored holiday.
        validatePeriod(
                request.getDate() != null ? request.getDate() : holiday.getDate(),
                request.getEndDate() != null ? request.getEndDate() : holiday.getEndDate(),
                request.getRecurring() != null ? request.getRecurring() : holiday.isRecurring(),
                id
        );
        trimNameIfSent(request);

        holidayMapper.updateEntity(holiday, request);
        return holidayMapper.toResponse(holidayRepository.save(holiday));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        HolidaysEntity holiday = findActiveOrThrow(id);
        SoftDeleteHelper.prepareForDeletion(holiday);
        holidayRepository.delete(holiday);
    }

    @Transactional(readOnly = true)
    @Override
    public Set<LocalDate> getNonPlayDatesBetween(UUID organizationId, LocalDate from, LocalDate to) {
        return holidayRepository.findAllActiveOfOrganization(organizationId)
                .stream()
                .flatMap(holiday -> holiday.occurrencesBetween(from, to).stream())
                .collect(Collectors.toSet());
    }

    private HolidaysEntity findActiveOrThrow(UUID id) {
        return holidayRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Holiday not found with id: " + id));
    }

    /** The name is optional in an update; when sent it is trimmed in place so the mapper copies the clean value. */
    private void trimNameIfSent(UpdateHolidayRequest request) {
        if (request.getName() != null) {
            request.setName(request.getName().trim());
        }
    }

    /**
     * Moving a holiday without saying where it ends keeps how long it lasts, so the stored end date moves along
     * with it (otherwise a single day moved to a later date would end before it starts).
     */
    private void keepDurationWhenMoved(UpdateHolidayRequest request, HolidaysEntity holiday) {
        if (request.getDate() != null && request.getEndDate() == null) {
            request.setEndDate(request.getDate().plusDays(holiday.daysAfterStart()));
        }
    }

    /**
     * The break must end on or after its first day (and not last more than a year), and none of its days may be
     * covered already: two holidays on the same day would be redundant, and a recurring one covers its days every
     * year, so the candidate is compared with every other active holiday. {@code endDate} may be null (a single
     * day); {@code ownId} is the holiday being edited, if any.
     */
    private void validatePeriod(LocalDate date, LocalDate endDate, boolean recurring, UUID ownId) {
        HolidaysEntity candidate = new HolidaysEntity();
        candidate.setDate(date);
        candidate.setEndDate(endDate);
        candidate.setRecurring(recurring);

        validateLength(candidate);
        validateNoClash(candidate, ownId);
    }

    private void validateLength(HolidaysEntity candidate) {
        long daysAfterStart = candidate.daysAfterStart();

        if (daysAfterStart < 0) {
            throw new FieldValidationException(END_DATE_FIELD, HolidayError.END_DATE_BEFORE_DATE.name());
        }
        if (daysAfterStart > HolidaysEntity.MAX_DAYS_AFTER_START) {
            throw new FieldValidationException(END_DATE_FIELD, HolidayError.RANGE_TOO_LONG.name());
        }
    }

    private void validateNoClash(HolidaysEntity candidate, UUID ownId) {
        boolean clashes = holidayRepository.findAllActive()
                .stream()
                .filter(existing -> !existing.getId().equals(ownId))
                .anyMatch(existing -> existing.clashesWith(candidate));

        if (clashes) {
            throw new FieldValidationException(DATE_FIELD, HolidayError.DATE_ALREADY_EXISTS.name());
        }
    }
}
