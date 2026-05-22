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

    boolean existsByName(String name);

    void delete(CourtsEntity court);

    void hardDelete(CourtsEntity court);
}
