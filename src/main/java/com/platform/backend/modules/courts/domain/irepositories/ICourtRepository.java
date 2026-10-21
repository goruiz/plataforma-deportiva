package com.platform.backend.modules.courts.domain.irepositories;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICourtRepository {

    CourtsEntity save(CourtsEntity court);

    Optional<CourtsEntity> findById(UUID id);

    List<CourtsEntity> findAllActive();

    Optional<CourtsEntity> findActiveById(UUID id);

    /** Whether an active court of that complex already has the name; a null complex means "courts without a complex". */
    boolean existsByNameInSportComplex(String name, UUID sportComplexId);

    boolean existsByNameInSportComplexExcludingId(String name, UUID sportComplexId, UUID id);

    boolean existsActiveBySportComplexId(UUID sportComplexId);

    void delete(CourtsEntity court);

    void hardDelete(CourtsEntity court);
}
