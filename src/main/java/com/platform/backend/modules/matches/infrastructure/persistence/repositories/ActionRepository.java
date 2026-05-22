package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.ActionsEntity;
import com.platform.backend.modules.matches.domain.irepositories.IActionRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.ActionsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ActionRepository implements IActionRepository {

    private final ActionsJpaRepository actionsJpaRepository;

    @Override
    public ActionsEntity save(ActionsEntity action) {
        return actionsJpaRepository.save(action);
    }

    @Override
    public Optional<ActionsEntity> findById(UUID id) {
        return actionsJpaRepository.findById(id);
    }

    @Override
    public List<ActionsEntity> findAllActive() {
        return actionsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<ActionsEntity> findActiveById(UUID id) {
        return actionsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return actionsJpaRepository.existsByName(name);
    }

    @Override
    public void delete(ActionsEntity action) {
        actionsJpaRepository.save(action);
    }

    @Override
    public void hardDelete(ActionsEntity action) {
        actionsJpaRepository.delete(action);
    }
}
