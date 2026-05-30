package com.platform.backend.modules.sportcomplexes.domain.irepositories;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ISportComplexRepository {

    SportComplexesEntity save(SportComplexesEntity sportComplex);

    Optional<SportComplexesEntity> findById(UUID id);

    List<SportComplexesEntity> findAllActive();

    Optional<SportComplexesEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(SportComplexesEntity sportComplex);

    void hardDelete(SportComplexesEntity sportComplex);
}
