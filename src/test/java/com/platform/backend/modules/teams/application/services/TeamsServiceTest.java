package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.teams.application.mappers.TeamMapper;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.exception.TeamError;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest.CreateTeamRequest;
import com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest.UpdateTeamRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamResponse.TeamResponse;
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
class TeamsServiceTest {

    private static final int NAME_COLUMN_LENGTH = 150;

    @Mock private ITeamRepository teamRepository;
    @Mock private ICategoryRepository categoryRepository;
    @Mock private TeamMapper teamMapper;

    @InjectMocks
    private TeamsService teamsService;

    private TeamsEntity teamEntity;
    private TeamResponse teamResponse;
    private CreateTeamRequest createRequest;
    private UpdateTeamRequest updateRequest;

    @BeforeEach
    void setUp() {
        teamEntity = new TeamsEntity();
        teamEntity.setId(UUID.randomUUID());
        teamEntity.setName("Tigres");
        teamEntity.setLogoUrl("https://example.com/tigres.png");

        teamResponse = new TeamResponse();
        teamResponse.setId(teamEntity.getId());
        teamResponse.setName("Tigres");

        createRequest = new CreateTeamRequest();
        createRequest.setName("  Tigres  ");

        updateRequest = new UpdateTeamRequest();
        updateRequest.setName("Leones");
    }

    private void assertRejectedField(Throwable error, String field, TeamError reason) {
        assertThat(error)
                .isInstanceOf(FieldValidationException.class)
                .satisfies(rejection -> assertThat(((FieldValidationException) rejection).getFieldErrors())
                        .containsEntry(field, reason.name()));
    }

    // ─── getAll / getById ───────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturnMappedList() {
        when(teamRepository.findAllActive()).thenReturn(List.of(teamEntity));
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        assertThat(teamsService.getAll()).containsExactly(teamResponse);
    }

    @Test
    void getById_whenFound_thenReturnMappedResponse() {
        UUID id = teamEntity.getId();
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        assertThat(teamsService.getById(id)).isSameAs(teamResponse);
    }

    @Test
    void getById_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(teamRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamsService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenNameIsFree_thenSaveWithTrimmedNameAndNoLogoWhenBlank() {
        createRequest.setLogoUrl("   ");
        when(teamRepository.existsByName("Tigres")).thenReturn(false);
        when(teamMapper.toEntity(createRequest)).thenReturn(teamEntity);
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        TeamResponse result = teamsService.create(createRequest);

        assertThat(result).isSameAs(teamResponse);
        assertThat(teamEntity.getName()).isEqualTo("Tigres");
        assertThat(teamEntity.getLogoUrl()).isNull();
    }

    @Test
    void create_whenCategoryExists_thenSaveTeam() {
        UUID categoryId = UUID.randomUUID();
        createRequest.setCategoryId(categoryId);
        when(teamRepository.existsByName("Tigres")).thenReturn(false);
        when(categoryRepository.findActiveById(categoryId)).thenReturn(Optional.of(new CategoriesEntity()));
        when(teamMapper.toEntity(createRequest)).thenReturn(teamEntity);
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        assertThat(teamsService.create(createRequest)).isSameAs(teamResponse);
    }

    @Test
    void create_whenNameAlreadyExists_thenRejectTheNameField() {
        when(teamRepository.existsByName("Tigres")).thenReturn(true);

        assertThatThrownBy(() -> teamsService.create(createRequest))
                .satisfies(error -> assertRejectedField(error, "name", TeamError.NAME_ALREADY_EXISTS));

        verify(teamRepository, never()).save(any());
    }

    @Test
    void create_whenCategoryDoesNotExist_thenRejectTheCategoryField() {
        UUID categoryId = UUID.randomUUID();
        createRequest.setCategoryId(categoryId);
        when(teamRepository.existsByName("Tigres")).thenReturn(false);
        when(categoryRepository.findActiveById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamsService.create(createRequest))
                .satisfies(error -> assertRejectedField(error, "categoryId", TeamError.CATEGORY_NOT_FOUND));

        verify(teamRepository, never()).save(any());
    }

    @Test
    void create_whenNoCategoryIsSent_thenSkipTheCategoryLookup() {
        when(teamRepository.existsByName("Tigres")).thenReturn(false);
        when(teamMapper.toEntity(createRequest)).thenReturn(teamEntity);
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        teamsService.create(createRequest);

        verify(categoryRepository, never()).findActiveById(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenNameIsFree_thenMapAndReturnUpdatedResponse() {
        UUID id = teamEntity.getId();
        updateRequest.setName("  Leones  ");
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(teamRepository.existsByNameExcludingId("Leones", id)).thenReturn(false);
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        TeamResponse result = teamsService.update(id, updateRequest);

        assertThat(result).isSameAs(teamResponse);
        assertThat(updateRequest.getName()).isEqualTo("Leones");
        verify(teamMapper).updateEntity(teamEntity, updateRequest);
    }

    @Test
    void update_whenNameBelongsToAnotherTeam_thenRejectTheNameField() {
        UUID id = teamEntity.getId();
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(teamRepository.existsByNameExcludingId("Leones", id)).thenReturn(true);

        assertThatThrownBy(() -> teamsService.update(id, updateRequest))
                .satisfies(error -> assertRejectedField(error, "name", TeamError.NAME_ALREADY_EXISTS));

        verify(teamRepository, never()).save(any());
    }

    @Test
    void update_whenNameIsNotSent_thenSkipUniquenessCheck() {
        UUID id = teamEntity.getId();
        updateRequest.setName(null);
        updateRequest.setStatus("INACTIVE");
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        teamsService.update(id, updateRequest);

        verify(teamRepository, never()).existsByNameExcludingId(any(), any());
    }

    @Test
    void update_whenStatusIsNotOneOfTheKnownOnes_thenRejectTheStatusField() {
        UUID id = teamEntity.getId();
        updateRequest.setName(null);
        updateRequest.setStatus("BANNED");
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));

        assertThatThrownBy(() -> teamsService.update(id, updateRequest))
                .satisfies(error -> assertRejectedField(error, "status", TeamError.INVALID_STATUS));

        verify(teamRepository, never()).save(any());
    }

    @Test
    void update_whenCategoryDoesNotExist_thenRejectTheCategoryField() {
        UUID id = teamEntity.getId();
        UUID categoryId = UUID.randomUUID();
        updateRequest.setName(null);
        updateRequest.setCategoryId(categoryId);
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(categoryRepository.findActiveById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamsService.update(id, updateRequest))
                .satisfies(error -> assertRejectedField(error, "categoryId", TeamError.CATEGORY_NOT_FOUND));

        verify(teamRepository, never()).save(any());
    }

    @Test
    void update_whenLogoIsBlank_thenRemoveTheLogo() {
        UUID id = teamEntity.getId();
        updateRequest.setName(null);
        updateRequest.setLogoUrl("");
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        teamsService.update(id, updateRequest);

        assertThat(teamEntity.getLogoUrl()).isNull();
    }

    @Test
    void update_whenLogoIsNotSent_thenKeepTheLogo() {
        UUID id = teamEntity.getId();
        updateRequest.setName(null);
        updateRequest.setStatus("ACTIVE");
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));
        when(teamRepository.save(teamEntity)).thenReturn(teamEntity);
        when(teamMapper.toResponse(teamEntity)).thenReturn(teamResponse);

        teamsService.update(id, updateRequest);

        assertThat(teamEntity.getLogoUrl()).isEqualTo("https://example.com/tigres.png");
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(teamRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamsService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_thenMarkDeletedAndFreeTheNameForReuse() {
        UUID id = teamEntity.getId();
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));

        teamsService.delete(id);

        assertThat(teamEntity.getDeletedAt()).isNotNull();
        assertThat(teamEntity.getName()).isEqualTo("Tigres_DELETED_" + id);
        verify(teamRepository).delete(teamEntity);
    }

    @Test
    void delete_whenTheNameIsAsLongAsTheColumn_thenTheFreedNameStillFits() {
        UUID id = teamEntity.getId();
        teamEntity.setName("T".repeat(NAME_COLUMN_LENGTH));
        when(teamRepository.findActiveById(id)).thenReturn(Optional.of(teamEntity));

        teamsService.delete(id);

        assertThat(teamEntity.getName()).hasSizeLessThanOrEqualTo(NAME_COLUMN_LENGTH).endsWith("_DELETED_" + id);
    }

    @Test
    void delete_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(teamRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamsService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(teamRepository, never()).delete(any());
    }
}
