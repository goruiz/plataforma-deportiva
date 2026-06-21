package com.platform.backend.modules.sportcomplexes.infrastructure.persistence.repositories;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.modules.sportcomplexes.infrastructure.persistence.jpa.SportComplexesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SportComplexRepository implements ISportComplexRepository {

    private final SportComplexesJpaRepository sportComplexesJpaRepository;

    @Override
    public SportComplexesEntity save(SportComplexesEntity sportComplex) {
        return sportComplexesJpaRepository.save(sportComplex);
    }

    @Override
    public Optional<SportComplexesEntity> findById(UUID id) {
        return sportComplexesJpaRepository.findById(id);
    }

    @Override
    public List<SportComplexesEntity> findAllActive() {
        return sportComplexesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<SportComplexesEntity> findActiveById(UUID id) {
        return sportComplexesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return sportComplexesJpaRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public void delete(SportComplexesEntity sportComplex) {
        sportComplexesJpaRepository.save(sportComplex);
    }

    @Override
    public void hardDelete(SportComplexesEntity sportComplex) {
        sportComplexesJpaRepository.delete(sportComplex);
    }
}

