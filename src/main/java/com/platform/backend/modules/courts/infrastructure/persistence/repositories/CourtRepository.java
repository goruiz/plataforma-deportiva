package com.platform.backend.modules.courts.infrastructure.persistence.repositories;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.courts.infrastructure.persistence.jpa.CourtsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CourtRepository implements ICourtRepository {

    private final CourtsJpaRepository courtsJpaRepository;

    @Override
    public CourtsEntity save(CourtsEntity court) {
        return courtsJpaRepository.save(court);
    }

    @Override
    public Optional<CourtsEntity> findById(UUID id) {
        return courtsJpaRepository.findById(id);
    }

    @Override
    public List<CourtsEntity> findAllActive() {
        return courtsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<CourtsEntity> findActiveById(UUID id) {
        return courtsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return courtsJpaRepository.existsByName(name);
    }

    @Override
    public void delete(CourtsEntity court) {
        courtsJpaRepository.save(court);
    }

    @Override
    public void hardDelete(CourtsEntity court) {
        courtsJpaRepository.delete(court);
    }
}
