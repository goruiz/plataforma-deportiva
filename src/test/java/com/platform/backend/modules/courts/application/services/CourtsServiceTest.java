package com.platform.backend.modules.courts.application.services;

import com.platform.backend.modules.courts.application.mappers.CourtMapper;
import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.domain.exception.CourtError;
import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest.CreateCourtRequest;
import com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest.UpdateCourtRequest;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
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
class CourtsServiceTest {

    @Mock private ICourtRepository courtRepository;
    @Mock private ISportComplexRepository sportComplexRepository;
    @Mock private CourtMapper courtMapper;

    @InjectMocks
    private CourtsService courtsService;

    private SportComplexesEntity northComplex;
    private SportComplexesEntity southComplex;
    private CourtsEntity courtEntity;
    private CourtResponse courtResponse;
    private CreateCourtRequest createRequest;
    private UpdateCourtRequest updateRequest;

    @BeforeEach
    void setUp() {
        northComplex = complexNamed("Complejo Norte");
        southComplex = complexNamed("Complejo Sur");

        courtEntity = new CourtsEntity();
        courtEntity.setId(UUID.randomUUID());
        courtEntity.setName("Cancha 1");
        courtEntity.setSportComplex(northComplex);

        courtResponse = new CourtResponse();

        createRequest = new CreateCourtRequest();
        createRequest.setName("  Cancha 1  ");
        createRequest.setIdSportComplex(northComplex.getId());

        updateRequest = new UpdateCourtRequest();
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenTheComplexBelongsToAnOrganization_thenTheCourtBelongsToIt() {
        UUID organizationId = UUID.randomUUID();
        northComplex.setOrganizationId(organizationId);
        when(sportComplexRepository.findActiveById(northComplex.getId())).thenReturn(Optional.of(northComplex));
        CourtsEntity newCourt = new CourtsEntity();
        when(courtMapper.toEntity(createRequest)).thenReturn(newCourt);
        when(courtRepository.save(newCourt)).thenReturn(newCourt);

        courtsService.create(createRequest);

        assertThat(newCourt.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void create_whenThereIsNoComplex_thenTheCourtHasNoOrganizationOfItsOwnYet() {
        createRequest.setIdSportComplex(null);
        CourtsEntity newCourt = new CourtsEntity();
        when(courtMapper.toEntity(createRequest)).thenReturn(newCourt);
        when(courtRepository.save(newCourt)).thenReturn(newCourt);

        courtsService.create(createRequest);

        assertThat(newCourt.getOrganizationId()).isNull();
    }

    @Test
    void create_whenNameIsFreeInTheComplex_thenSaveWithTrimmedNameAndComplex() {
        when(sportComplexRepository.findActiveById(northComplex.getId())).thenReturn(Optional.of(northComplex));
        when(courtRepository.existsByNameInSportComplex("Cancha 1", northComplex.getId())).thenReturn(false);
        CourtsEntity newCourt = new CourtsEntity();
        when(courtMapper.toEntity(createRequest)).thenReturn(newCourt);
        when(courtRepository.save(newCourt)).thenReturn(newCourt);
        when(courtMapper.toResponse(newCourt)).thenReturn(courtResponse);

        CourtResponse result = courtsService.create(createRequest);

        assertThat(result).isSameAs(courtResponse);
        assertThat(newCourt.getName()).isEqualTo("Cancha 1");
        assertThat(newCourt.getSportComplex()).isSameAs(northComplex);
    }

    @Test
    void create_whenNameIsTakenInTheSameComplex_thenThrowFieldValidationException() {
        when(sportComplexRepository.findActiveById(northComplex.getId())).thenReturn(Optional.of(northComplex));
        when(courtRepository.existsByNameInSportComplex("Cancha 1", northComplex.getId())).thenReturn(true);

        assertThatThrownBy(() -> courtsService.create(createRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("name", CourtError.NAME_ALREADY_EXISTS.name()));

        verify(courtRepository, never()).save(any());
    }

    @Test
    void create_whenThereIsNoComplex_thenCheckTheNamesOfCourtsWithoutComplex() {
        createRequest.setIdSportComplex(null);
        when(courtRepository.existsByNameInSportComplex("Cancha 1", null)).thenReturn(true);

        assertThatThrownBy(() -> courtsService.create(createRequest))
                .isInstanceOf(FieldValidationException.class);

        verify(courtRepository, never()).save(any());
    }

    @Test
    void create_whenTheComplexDoesNotExist_thenThrowEntityNotFoundException() {
        UUID unknownComplexId = UUID.randomUUID();
        createRequest.setIdSportComplex(unknownComplexId);
        when(sportComplexRepository.findActiveById(unknownComplexId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtsService.create(createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(unknownComplexId.toString());

        verify(courtRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenRenamedToAFreeName_thenCopyTrimmedNameAndSave() {
        updateRequest.setName("  Cancha 2  ");
        when(courtRepository.findActiveById(courtEntity.getId())).thenReturn(Optional.of(courtEntity));
        when(courtRepository.existsByNameInSportComplexExcludingId("Cancha 2", northComplex.getId(), courtEntity.getId()))
                .thenReturn(false);
        when(courtRepository.save(courtEntity)).thenReturn(courtEntity);

        courtsService.update(courtEntity.getId(), updateRequest);

        assertThat(updateRequest.getName()).isEqualTo("Cancha 2");
        verify(courtMapper).updateEntity(courtEntity, updateRequest);
    }

    @Test
    void update_whenRenamedToANameTakenInTheComplex_thenThrowFieldValidationException() {
        updateRequest.setName("Cancha 2");
        when(courtRepository.findActiveById(courtEntity.getId())).thenReturn(Optional.of(courtEntity));
        when(courtRepository.existsByNameInSportComplexExcludingId("Cancha 2", northComplex.getId(), courtEntity.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> courtsService.update(courtEntity.getId(), updateRequest))
                .isInstanceOf(FieldValidationException.class);

        verify(courtRepository, never()).save(any());
    }

    @Test
    void update_whenMovedToAComplexThatAlreadyHasThatName_thenThrowFieldValidationException() {
        updateRequest.setIdSportComplex(southComplex.getId());
        when(courtRepository.findActiveById(courtEntity.getId())).thenReturn(Optional.of(courtEntity));
        when(sportComplexRepository.findActiveById(southComplex.getId())).thenReturn(Optional.of(southComplex));
        when(courtRepository.existsByNameInSportComplexExcludingId("Cancha 1", southComplex.getId(), courtEntity.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> courtsService.update(courtEntity.getId(), updateRequest))
                .isInstanceOf(FieldValidationException.class);

        verify(courtRepository, never()).save(any());
    }

    @Test
    void update_whenMovedToAnotherComplex_thenAssignTheNewComplex() {
        updateRequest.setIdSportComplex(southComplex.getId());
        when(courtRepository.findActiveById(courtEntity.getId())).thenReturn(Optional.of(courtEntity));
        when(sportComplexRepository.findActiveById(southComplex.getId())).thenReturn(Optional.of(southComplex));
        when(courtRepository.existsByNameInSportComplexExcludingId("Cancha 1", southComplex.getId(), courtEntity.getId()))
                .thenReturn(false);
        when(courtRepository.save(courtEntity)).thenReturn(courtEntity);

        courtsService.update(courtEntity.getId(), updateRequest);

        assertThat(courtEntity.getSportComplex()).isSameAs(southComplex);
    }

    @Test
    void update_whenNameAndComplexStayTheSame_thenSkipUniquenessCheck() {
        updateRequest.setName("Cancha 1");
        updateRequest.setIdSportComplex(northComplex.getId());
        updateRequest.setDescription("Techada");
        when(courtRepository.findActiveById(courtEntity.getId())).thenReturn(Optional.of(courtEntity));
        when(sportComplexRepository.findActiveById(northComplex.getId())).thenReturn(Optional.of(northComplex));
        when(courtRepository.save(courtEntity)).thenReturn(courtEntity);

        courtsService.update(courtEntity.getId(), updateRequest);

        verify(courtRepository, never()).existsByNameInSportComplexExcludingId(any(), any(), any());
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(courtRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtsService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private SportComplexesEntity complexNamed(String name) {
        SportComplexesEntity complex = new SportComplexesEntity();
        complex.setId(UUID.randomUUID());
        complex.setName(name);
        return complex;
    }
}
