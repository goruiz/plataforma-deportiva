package com.platform.backend.modules.holidays.infrastructure.persistence.repositories;

import com.platform.backend.modules.holidays.domain.entities.HolidaysEntity;
import com.platform.backend.modules.holidays.domain.irepositories.IHolidayRepository;
import com.platform.backend.modules.holidays.infrastructure.persistence.jpa.HolidaysJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HolidayRepository implements IHolidayRepository {

    private final HolidaysJpaRepository holidaysJpaRepository;

    @Override
    public HolidaysEntity save(HolidaysEntity holiday) {
        return holidaysJpaRepository.save(holiday);
    }

    @Override
    public List<HolidaysEntity> findAllActive() {
        return holidaysJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<HolidaysEntity> findActiveById(UUID id) {
        return holidaysJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(HolidaysEntity holiday) {
        holidaysJpaRepository.save(holiday);
    }
}
