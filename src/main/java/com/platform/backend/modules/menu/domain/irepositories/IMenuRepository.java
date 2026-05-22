package com.platform.backend.modules.menu.domain.irepositories;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMenuRepository {

    MenuEntity save(MenuEntity menu);

    Optional<MenuEntity> findById(UUID id);

    List<MenuEntity> findAllActive();

    Optional<MenuEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(MenuEntity menu);

    void hardDelete(MenuEntity menu);
}
