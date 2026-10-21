package com.platform.backend.modules.sportcomplexes.application.services;

import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.sportcomplexes.application.mappers.SportComplexMapper;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.exception.SportComplexError;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest.CreateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest.UpdateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportComplexesServiceTest {

    private static final double LATITUDE = -0.1807;
    private static final double LONGITUDE = -78.4678;

    @Mock private ISportComplexRepository sportComplexRepository;
    @Mock private ICourtRepository courtRepository;
    @Mock private SportComplexMapper sportComplexMapper;

    @InjectMocks
    private SportComplexesService sportComplexesService;

    private SportComplexesEntity complexEntity;
    private SportComplexResponse complexResponse;
    private CreateSportComplexRequest createRequest;
    private UpdateSportComplexRequest updateRequest;

    @BeforeEach
    void setUp() {
        complexEntity = new SportComplexesEntity();
        complexEntity.setId(UUID.randomUUID());
        complexEntity.setName("Complejo Norte");

        complexResponse = new SportComplexResponse();
        complexResponse.setId(complexEntity.getId());

        createRequest = new CreateSportComplexRequest();
        createRequest.setName("  Complejo Norte  ");
        createRequest.setLatitude(LATITUDE);
        createRequest.setLongitude(LONGITUDE);

        updateRequest = new UpdateSportComplexRequest();
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenNameIsUniqueAndCoordinatesComplete_thenSaveWithTrimmedName() {
        when(sportComplexRepository.existsByName("Complejo Norte")).thenReturn(false);
        when(sportComplexMapper.toEntity(createRequest)).thenReturn(complexEntity);
        when(sportComplexRepository.save(complexEntity)).thenReturn(complexEntity);
        when(sportComplexMapper.toResponse(complexEntity)).thenReturn(complexResponse);

        SportComplexResponse result = sportComplexesService.create(createRequest);

        assertThat(result).isSameAs(complexResponse);
        assertThat(complexEntity.getName()).isEqualTo("Complejo Norte");
    }

    @Test
    void create_whenNoCoordinatesAreSent_thenSaveWithoutLocation() {
        createRequest.setLatitude(null);
        createRequest.setLongitude(null);
        when(sportComplexRepository.existsByName("Complejo Norte")).thenReturn(false);
        when(sportComplexMapper.toEntity(createRequest)).thenReturn(complexEntity);
        when(sportComplexRepository.save(complexEntity)).thenReturn(complexEntity);

        sportComplexesService.create(createRequest);

        verify(sportComplexRepository).save(complexEntity);
    }

    @Test
    void create_whenNameAlreadyExists_thenThrowFieldValidationException() {
        when(sportComplexRepository.existsByName("Complejo Norte")).thenReturn(true);

        assertThatThrownBy(() -> sportComplexesService.create(createRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("name", SportComplexError.NAME_ALREADY_EXISTS.name()));

        verify(sportComplexRepository, never()).save(any());
    }

    @Test
    void create_whenOnlyLatitudeIsSent_thenPointAtTheMissingLongitude() {
        createRequest.setLongitude(null);
        when(sportComplexRepository.existsByName("Complejo Norte")).thenReturn(false);

        assertThatThrownBy(() -> sportComplexesService.create(createRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("longitude", SportComplexError.COORDINATES_INCOMPLETE.name()));

        verify(sportComplexRepository, never()).save(any());
    }

    @Test
    void create_whenOnlyLongitudeIsSent_thenPointAtTheMissingLatitude() {
        createRequest.setLatitude(null);
        when(sportComplexRepository.existsByName("Complejo Norte")).thenReturn(false);

        assertThatThrownBy(() -> sportComplexesService.create(createRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("latitude", SportComplexError.COORDINATES_INCOMPLETE.name()));
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenNameIsFree_thenCopyTrimmedNameAndSave() {
        UUID id = complexEntity.getId();
        updateRequest.setName("  Complejo Sur  ");
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));
        when(sportComplexRepository.existsByNameExcludingId("Complejo Sur", id)).thenReturn(false);
        when(sportComplexRepository.save(complexEntity)).thenReturn(complexEntity);

        sportComplexesService.update(id, updateRequest);

        assertThat(updateRequest.getName()).isEqualTo("Complejo Sur");
        verify(sportComplexMapper).updateEntity(complexEntity, updateRequest);
    }

    @Test
    void update_whenNameBelongsToAnotherComplex_thenThrowFieldValidationException() {
        UUID id = complexEntity.getId();
        updateRequest.setName("Complejo Sur");
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));
        when(sportComplexRepository.existsByNameExcludingId("Complejo Sur", id)).thenReturn(true);

        assertThatThrownBy(() -> sportComplexesService.update(id, updateRequest))
                .isInstanceOf(FieldValidationException.class);

        verify(sportComplexRepository, never()).save(any());
    }

    @Test
    void update_whenNameIsNotSent_thenSkipUniquenessCheck() {
        UUID id = complexEntity.getId();
        updateRequest.setDescription("Nueva descripción");
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));
        when(sportComplexRepository.save(complexEntity)).thenReturn(complexEntity);

        sportComplexesService.update(id, updateRequest);

        verify(sportComplexRepository, never()).existsByNameExcludingId(any(), any());
    }

    @Test
    void update_whenComplexHasNoLocationAndOnlyLatitudeIsSent_thenRefuse() {
        UUID id = complexEntity.getId();
        updateRequest.setLatitude(LATITUDE);
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));

        assertThatThrownBy(() -> sportComplexesService.update(id, updateRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("longitude", SportComplexError.COORDINATES_INCOMPLETE.name()));

        verify(sportComplexRepository, never()).save(any());
    }

    @Test
    void update_whenComplexAlreadyHasLocationAndOnlyLatitudeIsSent_thenKeepTheLongitude() {
        UUID id = complexEntity.getId();
        complexEntity.setLatitude(LATITUDE);
        complexEntity.setLongitude(LONGITUDE);
        updateRequest.setLatitude(-0.2);
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));
        when(sportComplexRepository.save(complexEntity)).thenReturn(complexEntity);

        sportComplexesService.update(id, updateRequest);

        verify(sportComplexMapper).updateEntity(complexEntity, updateRequest);
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sportComplexesService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenItHasNoCourts_thenMarkDeleted() {
        UUID id = complexEntity.getId();
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));
        when(courtRepository.existsActiveBySportComplexId(id)).thenReturn(false);

        sportComplexesService.delete(id);

        assertThat(complexEntity.getDeletedAt()).isNotNull();
        verify(sportComplexRepository).delete(complexEntity);
    }

    @Test
    void delete_whenItStillHasCourts_thenRefuseAndKeepTheComplex() {
        UUID id = complexEntity.getId();
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.of(complexEntity));
        when(courtRepository.existsActiveBySportComplexId(id)).thenReturn(true);

        assertThatThrownBy(() -> sportComplexesService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(SportComplexError.IN_USE.name());

        assertThat(complexEntity.getDeletedAt()).isNull();
        verify(sportComplexRepository, never()).delete(any());
    }

    @Test
    void delete_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(sportComplexRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sportComplexesService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
