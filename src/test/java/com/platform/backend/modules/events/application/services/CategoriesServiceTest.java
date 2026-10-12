package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.mappers.CategoryMapper;
import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.exception.CategoryError;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsCategoriesRepository;
import com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest.CreateCategoryRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest.UpdateCategoryRequest;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriesServiceTest {

    private static final int NAME_COLUMN_LENGTH = 100;

    @Mock private ICategoryRepository categoryRepository;
    @Mock private IEventsCategoriesRepository eventsCategoriesRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoriesService categoriesService;

    private CategoriesEntity categoryEntity;
    private CategoryResponse categoryResponse;
    private CreateCategoryRequest createRequest;
    private UpdateCategoryRequest updateRequest;

    @BeforeEach
    void setUp() {
        categoryEntity = new CategoriesEntity();
        categoryEntity.setId(UUID.randomUUID());
        categoryEntity.setName("Sub-15");
        categoryEntity.setDescription("Jugadores de hasta 15 años");

        categoryResponse = new CategoryResponse();
        categoryResponse.setId(categoryEntity.getId());
        categoryResponse.setName("Sub-15");

        createRequest = new CreateCategoryRequest();
        createRequest.setName("  Sub-15  ");
        createRequest.setDescription("Jugadores de hasta 15 años");

        updateRequest = new UpdateCategoryRequest();
        updateRequest.setName("  Sub-17  ");
    }

    // ─── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturnMappedList() {
        when(categoryRepository.findAllActive()).thenReturn(List.of(categoryEntity));
        when(categoryMapper.toResponse(categoryEntity)).thenReturn(categoryResponse);

        assertThat(categoriesService.getAll()).containsExactly(categoryResponse);
    }

    // ─── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_whenFound_thenReturnMappedResponse() {
        UUID id = categoryEntity.getId();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(categoryMapper.toResponse(categoryEntity)).thenReturn(categoryResponse);

        assertThat(categoriesService.getById(id)).isSameAs(categoryResponse);
    }

    @Test
    void getById_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriesService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenNameIsUnique_thenSaveWithTrimmedNameAndReturnResponse() {
        when(categoryRepository.existsByName("Sub-15")).thenReturn(false);
        when(categoryMapper.toEntity(createRequest)).thenReturn(categoryEntity);
        when(categoryRepository.save(categoryEntity)).thenReturn(categoryEntity);
        when(categoryMapper.toResponse(categoryEntity)).thenReturn(categoryResponse);

        CategoryResponse result = categoriesService.create(createRequest);

        assertThat(result).isSameAs(categoryResponse);
        assertThat(categoryEntity.getName()).isEqualTo("Sub-15");
        verify(categoryRepository).save(categoryEntity);
    }

    @Test
    void create_whenNameAlreadyExists_thenThrowFieldValidationException() {
        when(categoryRepository.existsByName("Sub-15")).thenReturn(true);

        assertThatThrownBy(() -> categoriesService.create(createRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("name", CategoryError.NAME_ALREADY_EXISTS.name()));

        verify(categoryRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenFoundAndNameFree_thenCopyTrimmedNameAndReturnResponse() {
        UUID id = categoryEntity.getId();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(categoryRepository.existsByNameExcludingId("Sub-17", id)).thenReturn(false);
        when(categoryRepository.save(categoryEntity)).thenReturn(categoryEntity);
        when(categoryMapper.toResponse(categoryEntity)).thenReturn(categoryResponse);

        CategoryResponse result = categoriesService.update(id, updateRequest);

        assertThat(result).isSameAs(categoryResponse);
        assertThat(updateRequest.getName()).isEqualTo("Sub-17");
        verify(categoryMapper).updateEntity(categoryEntity, updateRequest);
    }

    @Test
    void update_whenNameBelongsToAnotherCategory_thenThrowFieldValidationException() {
        UUID id = categoryEntity.getId();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(categoryRepository.existsByNameExcludingId("Sub-17", id)).thenReturn(true);

        assertThatThrownBy(() -> categoriesService.update(id, updateRequest))
                .isInstanceOf(FieldValidationException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_whenNameIsNotSent_thenSkipUniquenessCheck() {
        UUID id = categoryEntity.getId();
        updateRequest.setName(null);
        updateRequest.setDescription("Nueva descripción");
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(categoryRepository.save(categoryEntity)).thenReturn(categoryEntity);
        when(categoryMapper.toResponse(categoryEntity)).thenReturn(categoryResponse);

        categoriesService.update(id, updateRequest);

        verify(categoryRepository, never()).existsByNameExcludingId(any(), any());
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriesService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenNothingUsesIt_thenMarkDeletedAndFreeTheName() {
        UUID id = categoryEntity.getId();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(eventsCategoriesRepository.existsActiveByCategoryId(id)).thenReturn(false);
        when(teamRepository.existsActiveByCategoryId(id)).thenReturn(false);

        categoriesService.delete(id);

        assertThat(categoryEntity.getDeletedAt()).isNotNull();
        assertThat(categoryEntity.getName()).isEqualTo("Sub-15_DELETED_" + id);
        verify(categoryRepository).delete(categoryEntity);
    }

    @Test
    void delete_whenNameIsAsLongAsTheColumn_thenTaggedNameStillFitsTheColumn() {
        UUID id = categoryEntity.getId();
        categoryEntity.setName("x".repeat(NAME_COLUMN_LENGTH));
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));

        categoriesService.delete(id);

        assertThat(categoryEntity.getName()).hasSize(NAME_COLUMN_LENGTH).endsWith("_DELETED_" + id);
    }

    @Test
    void delete_whenAnEventUsesIt_thenRefuseAndKeepTheCategory() {
        UUID id = categoryEntity.getId();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(eventsCategoriesRepository.existsActiveByCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> categoriesService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CategoryError.IN_USE.name());

        assertThat(categoryEntity.getDeletedAt()).isNull();
        assertThat(categoryEntity.getName()).isEqualTo("Sub-15");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_whenATeamUsesIt_thenRefuseAndKeepTheCategory() {
        UUID id = categoryEntity.getId();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.of(categoryEntity));
        when(eventsCategoriesRepository.existsActiveByCategoryId(id)).thenReturn(false);
        when(teamRepository.existsActiveByCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> categoriesService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CategoryError.IN_USE.name());

        assertThat(categoryEntity.getDeletedAt()).isNull();
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriesService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
