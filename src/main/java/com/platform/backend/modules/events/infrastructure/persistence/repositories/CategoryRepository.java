package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.CategoriesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepository implements ICategoryRepository {

    private final CategoriesJpaRepository categoriesJpaRepository;

    @Override
    public CategoriesEntity save(CategoriesEntity category) {
        return categoriesJpaRepository.save(category);
    }

    @Override
    public Optional<CategoriesEntity> findById(UUID id) {
        return categoriesJpaRepository.findById(id);
    }

    @Override
    public List<CategoriesEntity> findAllActive() {
        return categoriesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<CategoriesEntity> findActiveById(UUID id) {
        return categoriesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return categoriesJpaRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public void delete(CategoriesEntity category) {
        categoriesJpaRepository.save(category);
    }

    @Override
    public void hardDelete(CategoriesEntity category) {
        categoriesJpaRepository.delete(category);
    }
}

