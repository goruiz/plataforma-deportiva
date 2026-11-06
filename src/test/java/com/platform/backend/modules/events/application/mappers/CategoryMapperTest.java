package com.platform.backend.modules.events.application.mappers;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = Mappers.getMapper(CategoryMapper.class);

    @Test
    void toResponse_whenCategoryBelongsToAnOrganization_thenExposeIt() {
        UUID organizationId = UUID.randomUUID();
        CategoriesEntity category = new CategoriesEntity();
        category.setName("Sub 17");
        category.setOrganizationId(organizationId);

        CategoryResponse response = mapper.toResponse(category);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void toResponse_whenCategoryIsThePlatformOwners_thenOrganizationIsNull() {
        CategoriesEntity category = new CategoriesEntity();
        category.setName("Categoría del propietario");

        CategoryResponse response = mapper.toResponse(category);

        assertThat(response.getOrganizationId()).isNull();
    }
}
