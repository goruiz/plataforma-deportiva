package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.ICategoriesService;
import com.platform.backend.modules.events.application.mappers.CategoryMapper;
import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest.CreateCategoryRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest.UpdateCategoryRequest;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriesService implements ICategoriesService {

    private final ICategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllActive()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(UUID id) {
        CategoriesEntity category = categoryRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Category name already exists");
        }
        CategoriesEntity category = categoryMapper.toEntity(request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        CategoriesEntity category = categoryRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        categoryMapper.updateEntity(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(UUID id) {
        CategoriesEntity category = categoryRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.delete(category);
    }

    @Override
    public void hardDelete(UUID id) {
        CategoriesEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        categoryRepository.hardDelete(category);
    }
}
