package com.platform.backend.modules.menu.infrastructure.persistence.repositories;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.infrastructure.persistence.jpa.MenuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MenuRepository implements IMenuRepository {

    private final MenuJpaRepository menuJpaRepository;

    @Override
    public MenuEntity save(MenuEntity menu) {
        return menuJpaRepository.save(menu);
    }

    @Override
    public Optional<MenuEntity> findById(UUID id) {
        return menuJpaRepository.findById(id);
    }

    @Override
    public List<MenuEntity> findAllActive() {
        return menuJpaRepository.findAllByDeletedAtIsNullOrderByOrderAsc();
    }

    @Override
    public Optional<MenuEntity> findActiveById(UUID id) {
        return menuJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return menuJpaRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public void delete(MenuEntity menu) {
        menuJpaRepository.save(menu);
    }

    @Override
    public void hardDelete(MenuEntity menu) {
        menuJpaRepository.delete(menu);
    }
}

