package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.ActionsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IActionRepository {

    ActionsEntity save(ActionsEntity action);

    Optional<ActionsEntity> findById(UUID id);

    List<ActionsEntity> findAllActive();

    Optional<ActionsEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(ActionsEntity action);

    void hardDelete(ActionsEntity action);
}
