package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.ICategoriesService;
import com.platform.backend.modules.events.application.mappers.CategoryMapper;
import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.exception.CategoryError;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsCategoriesRepository;
import com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest.CreateCategoryRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest.UpdateCategoryRequest;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriesService implements ICategoriesService {

    private static final String NAME_FIELD = "name";

    private final ICategoryRepository categoryRepository;
    private final IEventsCategoriesRepository eventsCategoriesRepository;
    private final ITeamRepository teamRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllActive()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CategoryResponse getById(UUID id) {
        return categoryMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw nameAlreadyExists();
        }
        CategoriesEntity category = categoryMapper.toEntity(request);
        category.setName(name);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    @Override
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        CategoriesEntity category = findActiveOrThrow(id);
        validateNameIsFree(request, id);
        categoryMapper.updateEntity(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        CategoriesEntity category = findActiveOrThrow(id);
        if (isInUse(id)) {
            throw new IllegalStateException(CategoryError.IN_USE.name());
        }
        SoftDeleteHelper.prepareForDeletion(category);
        categoryRepository.delete(category);
    }

    @Transactional
    @Override
    public void hardDelete(UUID id) {
        CategoriesEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        categoryRepository.hardDelete(category);
    }

    private CategoriesEntity findActiveOrThrow(UUID id) {
        return categoryRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    /** The name is optional in an update; when sent it is trimmed in place so the mapper copies the clean value. */
    private void validateNameIsFree(UpdateCategoryRequest request, UUID categoryId) {
        if (request.getName() == null) {
            return;
        }
        String name = request.getName().trim();
        if (categoryRepository.existsByNameExcludingId(name, categoryId)) {
            throw nameAlreadyExists();
        }
        request.setName(name);
    }

    // teams.category_id has no foreign key, so the database would not stop a delete that orphans teams.
    private boolean isInUse(UUID categoryId) {
        return eventsCategoriesRepository.existsActiveByCategoryId(categoryId)
                || teamRepository.existsActiveByCategoryId(categoryId);
    }

    private FieldValidationException nameAlreadyExists() {
        return new FieldValidationException(NAME_FIELD, CategoryError.NAME_ALREADY_EXISTS.name());
    }
}
