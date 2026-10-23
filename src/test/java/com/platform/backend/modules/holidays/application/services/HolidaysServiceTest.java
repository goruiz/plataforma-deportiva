package com.platform.backend.modules.holidays.application.services;

import com.platform.backend.modules.holidays.application.mappers.HolidayMapper;
import com.platform.backend.modules.holidays.domain.entities.HolidaysEntity;
import com.platform.backend.modules.holidays.domain.exception.HolidayError;
import com.platform.backend.modules.holidays.domain.irepositories.IHolidayRepository;
import com.platform.backend.modules.holidays.presentation.requests.CreateHolidayRequest.CreateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.requests.UpdateHolidayRequest.UpdateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.responses.HolidayResponse.HolidayResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidaysServiceTest {

    @Mock private IHolidayRepository holidayRepository;
    @Mock private HolidayMapper holidayMapper;

    @InjectMocks
    private HolidaysService holidaysService;

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    private HolidaysEntity christmas;

    @BeforeEach
    void setUp() {
        christmas = holidayEntity("Navidad", LocalDate.of(2026, 12, 25), true);
    }

    private static HolidaysEntity holidayEntity(String name, LocalDate date, boolean recurring) {
        HolidaysEntity holiday = new HolidaysEntity();
        holiday.setId(UUID.randomUUID());
        holiday.setName(name);
        holiday.setDate(date);
        holiday.setRecurring(recurring);
        return holiday;
    }

    private static HolidaysEntity holidayEntity(String name, LocalDate date, LocalDate endDate, boolean recurring) {
        HolidaysEntity holiday = holidayEntity(name, date, recurring);
        holiday.setEndDate(endDate);
        return holiday;
    }

    private static CreateHolidayRequest createRequest(String name, LocalDate date, boolean recurring) {
        CreateHolidayRequest request = new CreateHolidayRequest();
        request.setName(name);
        request.setDate(date);
        request.setRecurring(recurring);
        return request;
    }

    private static CreateHolidayRequest createRangeRequest(String name, LocalDate date, LocalDate endDate) {
        CreateHolidayRequest request = createRequest(name, date, false);
        request.setEndDate(endDate);
        return request;
    }

    // ─── getAll / getById ───────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturnMappedList() {
        HolidayResponse response = new HolidayResponse();
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas));
        when(holidayMapper.toResponse(christmas)).thenReturn(response);

        assertThat(holidaysService.getAll()).containsExactly(response);
    }

    @Test
    void getById_whenNotFound_thenThrowEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(holidayRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidaysService.getById(id)).isInstanceOf(EntityNotFoundException.class);
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenDateIsFree_thenSaveWithTrimmedName() {
        CreateHolidayRequest request = createRequest("  Carnaval  ", LocalDate.of(2027, 2, 15), false);
        HolidaysEntity mapped = holidayEntity("  Carnaval  ", LocalDate.of(2027, 2, 15), false);
        HolidayResponse response = new HolidayResponse();
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas));
        when(holidayMapper.toEntity(request)).thenReturn(mapped);
        when(holidayRepository.save(mapped)).thenReturn(mapped);
        when(holidayMapper.toResponse(mapped)).thenReturn(response);

        assertThat(holidaysService.create(request)).isSameAs(response);
        assertThat(mapped.getName()).isEqualTo("Carnaval");
    }

    @Test
    void create_whenAnotherHolidayHasTheSameDate_thenFieldErrorOnDate() {
        CreateHolidayRequest request = createRequest("Otro", LocalDate.of(2026, 12, 25), false);
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas));

        assertThatThrownBy(() -> holidaysService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("date", HolidayError.DATE_ALREADY_EXISTS.name()));
        verify(holidayRepository, never()).save(any());
    }

    @Test
    void create_whenARecurringHolidayAlreadyCoversThatDayInAnotherYear_thenFieldErrorOnDate() {
        // Christmas recurs every year, so a one-off Christmas in 2031 says the same thing twice.
        CreateHolidayRequest request = createRequest("Navidad 2031", LocalDate.of(2031, 12, 25), false);
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas));

        assertThatThrownBy(() -> holidaysService.create(request)).isInstanceOf(FieldValidationException.class);
    }

    @Test
    void create_whenTheNewRecurringHolidayCoversAnExistingOneOff_thenFieldErrorOnDate() {
        HolidaysEntity oneOff = holidayEntity("Navidad 2027", LocalDate.of(2027, 12, 25), false);
        CreateHolidayRequest request = createRequest("Navidad", LocalDate.of(2020, 12, 25), true);
        when(holidayRepository.findAllActive()).thenReturn(List.of(oneOff));

        assertThatThrownBy(() -> holidaysService.create(request)).isInstanceOf(FieldValidationException.class);
    }

    @Test
    void create_whenItIsARangeWithFreeDays_thenSaveIt() {
        CreateHolidayRequest request = createRangeRequest("Carnaval", LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 10));
        HolidaysEntity mapped = holidayEntity("Carnaval", LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 10), false);
        HolidayResponse response = new HolidayResponse();
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas));
        when(holidayMapper.toEntity(request)).thenReturn(mapped);
        when(holidayRepository.save(mapped)).thenReturn(mapped);
        when(holidayMapper.toResponse(mapped)).thenReturn(response);

        assertThat(holidaysService.create(request)).isSameAs(response);
    }

    @Test
    void create_whenTheEndIsBeforeTheStart_thenFieldErrorOnEndDate() {
        CreateHolidayRequest request = createRangeRequest("Carnaval", LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 8));

        assertThatThrownBy(() -> holidaysService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("endDate", HolidayError.END_DATE_BEFORE_DATE.name()));
        verify(holidayRepository, never()).save(any());
    }

    @Test
    void create_whenTheBreakLastsMoreThanAYear_thenFieldErrorOnEndDate() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        // 366 days after the start is one more than the longest break allowed.
        CreateHolidayRequest request = createRangeRequest("Receso", start, start.plusDays(HolidaysEntity.MAX_DAYS_AFTER_START + 1));

        assertThatThrownBy(() -> holidaysService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("endDate", HolidayError.RANGE_TOO_LONG.name()));
    }

    @Test
    void create_whenTheBreakLastsExactlyTheLongestAllowed_thenSaveIt() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        CreateHolidayRequest request = createRangeRequest("Receso", start, start.plusDays(HolidaysEntity.MAX_DAYS_AFTER_START));
        HolidaysEntity mapped = holidayEntity("Receso", start, start.plusDays(HolidaysEntity.MAX_DAYS_AFTER_START), false);
        when(holidayRepository.findAllActive()).thenReturn(List.of());
        when(holidayMapper.toEntity(request)).thenReturn(mapped);
        when(holidayRepository.save(mapped)).thenReturn(mapped);
        when(holidayMapper.toResponse(mapped)).thenReturn(new HolidayResponse());

        holidaysService.create(request);

        verify(holidayRepository).save(mapped);
    }

    @Test
    void create_whenOneDayOfTheRangeIsAlreadyAHoliday_thenFieldErrorOnDate() {
        HolidaysEntity maintenance = holidayEntity("Mantenimiento", LocalDate.of(2027, 2, 9), false);
        CreateHolidayRequest request = createRangeRequest("Carnaval", LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 10));
        when(holidayRepository.findAllActive()).thenReturn(List.of(maintenance));

        assertThatThrownBy(() -> holidaysService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("date", HolidayError.DATE_ALREADY_EXISTS.name()));
        verify(holidayRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenASingleDayIsMovedWithoutAnEnd_thenItStaysASingleDay() {
        HolidaysEntity maintenance = holidayEntity("Mantenimiento", LocalDate.of(2027, 3, 1), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setDate(LocalDate.of(2027, 3, 5));
        when(holidayRepository.findActiveById(maintenance.getId())).thenReturn(Optional.of(maintenance));
        when(holidayRepository.findAllActive()).thenReturn(List.of(maintenance));
        when(holidayRepository.save(maintenance)).thenReturn(maintenance);

        holidaysService.update(maintenance.getId(), request);

        assertThat(request.getEndDate()).isEqualTo(LocalDate.of(2027, 3, 5));
    }

    @Test
    void update_whenARangeIsMovedWithoutAnEnd_thenItKeepsItsLength() {
        HolidaysEntity carnival = holidayEntity("Carnaval", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setDate(LocalDate.of(2027, 3, 10));
        when(holidayRepository.findActiveById(carnival.getId())).thenReturn(Optional.of(carnival));
        when(holidayRepository.findAllActive()).thenReturn(List.of(carnival));
        when(holidayRepository.save(carnival)).thenReturn(carnival);

        holidaysService.update(carnival.getId(), request);

        assertThat(request.getEndDate()).isEqualTo(LocalDate.of(2027, 3, 12));
    }

    @Test
    void update_whenARangeIsSentWithTheSameEndAsStart_thenItBecomesASingleDay() {
        HolidaysEntity carnival = holidayEntity("Carnaval", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setDate(LocalDate.of(2027, 3, 1));
        request.setEndDate(LocalDate.of(2027, 3, 1));
        when(holidayRepository.findActiveById(carnival.getId())).thenReturn(Optional.of(carnival));
        when(holidayRepository.findAllActive()).thenReturn(List.of(carnival));
        when(holidayRepository.save(carnival)).thenReturn(carnival);

        holidaysService.update(carnival.getId(), request);

        verify(holidayMapper).updateEntity(carnival, request);
        assertThat(request.getEndDate()).isEqualTo(LocalDate.of(2027, 3, 1));
    }

    @Test
    void update_whenTheNewEndIsBeforeTheStoredStart_thenFieldErrorOnEndDate() {
        HolidaysEntity carnival = holidayEntity("Carnaval", LocalDate.of(2027, 3, 5), LocalDate.of(2027, 3, 7), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setEndDate(LocalDate.of(2027, 3, 1));
        when(holidayRepository.findActiveById(carnival.getId())).thenReturn(Optional.of(carnival));

        assertThatThrownBy(() -> holidaysService.update(carnival.getId(), request))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("endDate", HolidayError.END_DATE_BEFORE_DATE.name()));
        verify(holidayRepository, never()).save(any());
    }

    @Test
    void update_whenStretchedOntoAnotherHoliday_thenFieldErrorOnDate() {
        HolidaysEntity carnival = holidayEntity("Carnaval", LocalDate.of(2027, 3, 1), false);
        HolidaysEntity maintenance = holidayEntity("Mantenimiento", LocalDate.of(2027, 3, 3), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setEndDate(LocalDate.of(2027, 3, 4));
        when(holidayRepository.findActiveById(carnival.getId())).thenReturn(Optional.of(carnival));
        when(holidayRepository.findAllActive()).thenReturn(List.of(carnival, maintenance));

        assertThatThrownBy(() -> holidaysService.update(carnival.getId(), request))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).containsEntry("date", HolidayError.DATE_ALREADY_EXISTS.name()));
    }

    @Test
    void update_whenOnlyTheNameChanges_thenItDoesNotClashWithItself() {
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setName("  Navidad del año  ");
        HolidayResponse response = new HolidayResponse();
        when(holidayRepository.findActiveById(christmas.getId())).thenReturn(Optional.of(christmas));
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas));
        when(holidayRepository.save(christmas)).thenReturn(christmas);
        when(holidayMapper.toResponse(christmas)).thenReturn(response);

        assertThat(holidaysService.update(christmas.getId(), request)).isSameAs(response);
        assertThat(request.getName()).isEqualTo("Navidad del año");
        verify(holidayMapper).updateEntity(christmas, request);
    }

    @Test
    void update_whenMovedOntoAnotherHoliday_thenFieldErrorOnDate() {
        HolidaysEntity newYear = holidayEntity("Año nuevo", LocalDate.of(2027, 1, 1), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setDate(LocalDate.of(2027, 1, 1));
        request.setRecurring(false);
        when(holidayRepository.findActiveById(christmas.getId())).thenReturn(Optional.of(christmas));
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas, newYear));

        assertThatThrownBy(() -> holidaysService.update(christmas.getId(), request))
                .isInstanceOf(FieldValidationException.class);
        verify(holidayRepository, never()).save(any());
    }

    @Test
    void update_whenOnlyRecurringChanges_thenTheStoredDateIsTheOneChecked() {
        HolidaysEntity oneOffOnChristmas2031 = holidayEntity("Evento", LocalDate.of(2031, 12, 25), false);
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setRecurring(true); // making Christmas 2026 recurring would now also cover the 2031 holiday
        when(holidayRepository.findActiveById(christmas.getId())).thenReturn(Optional.of(christmas));
        when(holidayRepository.findAllActive()).thenReturn(List.of(christmas, oneOffOnChristmas2031));

        assertThatThrownBy(() -> holidaysService.update(christmas.getId(), request))
                .isInstanceOf(FieldValidationException.class);
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(holidayRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidaysService.update(id, new UpdateHolidayRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_thenSoftDelete() {
        when(holidayRepository.findActiveById(christmas.getId())).thenReturn(Optional.of(christmas));

        holidaysService.delete(christmas.getId());

        assertThat(christmas.getDeletedAt()).isNotNull();
        verify(holidayRepository).delete(christmas);
    }

    @Test
    void delete_whenNotFound_thenThrowEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(holidayRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidaysService.delete(id)).isInstanceOf(EntityNotFoundException.class);
    }

    // ─── getNonPlayDatesBetween ─────────────────────────────────────────────────

    @Test
    void getNonPlayDatesBetween_whenHolidaysAreOneOffAndRecurring_thenEveryDayInTheRange() {
        HolidaysEntity oneOff = holidayEntity("Mantenimiento", LocalDate.of(2026, 11, 3), false);
        HolidaysEntity outOfRange = holidayEntity("Fuera", LocalDate.of(2030, 5, 1), false);
        when(holidayRepository.findAllActiveOfOrganization(ORGANIZATION_ID)).thenReturn(List.of(christmas, oneOff, outOfRange));

        Set<LocalDate> dates = holidaysService.getNonPlayDatesBetween(
                ORGANIZATION_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 12, 31));

        assertThat(dates).containsExactlyInAnyOrder(
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 25), LocalDate.of(2027, 12, 25));
    }

    @Test
    void getNonPlayDatesBetween_whenAHolidayIsARange_thenEveryDayOfItInsideTheWindow() {
        HolidaysEntity carnival = holidayEntity("Carnaval", LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 10), false);
        when(holidayRepository.findAllActiveOfOrganization(ORGANIZATION_ID)).thenReturn(List.of(carnival));

        Set<LocalDate> dates = holidaysService.getNonPlayDatesBetween(
                ORGANIZATION_ID, LocalDate.of(2027, 2, 9), LocalDate.of(2027, 3, 1));

        assertThat(dates).containsExactlyInAnyOrder(LocalDate.of(2027, 2, 9), LocalDate.of(2027, 2, 10));
    }

    @Test
    void getNonPlayDatesBetween_whenThereAreNoHolidays_thenEmpty() {
        when(holidayRepository.findAllActiveOfOrganization(ORGANIZATION_ID)).thenReturn(List.of());

        assertThat(holidaysService.getNonPlayDatesBetween(
                ORGANIZATION_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).isEmpty();
    }

    @Test
    void getNonPlayDatesBetween_whenTheOrganizationIsNull_thenAsksForTheHolidaysOfThePlatformOwner() {
        when(holidayRepository.findAllActiveOfOrganization(null)).thenReturn(List.of(christmas));

        Set<LocalDate> dates = holidaysService.getNonPlayDatesBetween(null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31));

        assertThat(dates).containsExactly(LocalDate.of(2026, 12, 25));
        verify(holidayRepository, never()).findAllActive();
    }
}
