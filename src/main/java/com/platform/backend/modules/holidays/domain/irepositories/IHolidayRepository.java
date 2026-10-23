package com.platform.backend.modules.holidays.domain.irepositories;

import com.platform.backend.modules.holidays.domain.entities.HolidaysEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IHolidayRepository {

    HolidaysEntity save(HolidaysEntity holiday);

    List<HolidaysEntity> findAllActive();

    Optional<HolidaysEntity> findActiveById(UUID id);

    /** The holidays of one organization; a null organization means the ones that belong to the platform owner. */
    List<HolidaysEntity> findAllActiveOfOrganization(UUID organizationId);

    void delete(HolidaysEntity holiday);
}
