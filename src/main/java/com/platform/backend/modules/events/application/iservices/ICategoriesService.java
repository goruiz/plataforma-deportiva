package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest.CreateCategoryRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest.UpdateCategoryRequest;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface ICategoriesService {

    List<CategoryResponse> getAll();

    CategoryResponse getById(UUID id);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(UUID id, UpdateCategoryRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
