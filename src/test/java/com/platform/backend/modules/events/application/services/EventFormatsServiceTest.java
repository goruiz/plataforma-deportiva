package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.mappers.EventFormatMapper;
import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.exception.EventFormatError;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
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
class EventFormatsServiceTest {

    @Mock private IEventFormatRepository eventFormatRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private EventFormatMapper eventFormatMapper;

    @InjectMocks
    private EventFormatsService eventFormatsService;

    private EventFormatsEntity eventFormatEntity;
    private EventFormatResponse eventFormatResponse;
    private CreateEventFormatRequest createRequest;
    private UpdateEventFormatRequest updateRequest;

    @BeforeEach
    void setUp() {
        eventFormatEntity = new EventFormatsEntity();
        eventFormatEntity.setId(UUID.randomUUID());
        eventFormatEntity.setName("Liga");
        eventFormatEntity.setDescription("Todos contra todos");

        eventFormatResponse = new EventFormatResponse();
        eventFormatResponse.setId(eventFormatEntity.getId());
        eventFormatResponse.setName("Liga");

        createRequest = new CreateEventFormatRequest();
        createRequest.setName("  Liga  ");
        createRequest.setDescription("Todos contra todos");

        updateRequest = new UpdateEventFormatRequest();
        updateRequest.setName("Eliminación directa");
    }

    // ─── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturnMappedList() {
        when(eventFormatRepository.findAllActive()).thenReturn(List.of(eventFormatEntity));
        when(eventFormatMapper.toResponse(eventFormatEntity)).thenReturn(eventFormatResponse);

        List<EventFormatResponse> result = eventFormatsService.getAll();

        assertThat(result).containsExactly(eventFormatResponse);
    }

    // ─── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_whenFound_thenReturnMappedResponse() {
        UUID id = eventFormatEntity.getId();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.of(eventFormatEntity));
        when(eventFormatMapper.toResponse(eventFormatEntity)).thenReturn(eventFormatResponse);

        assertThat(eventFormatsService.getById(id)).isSameAs(eventFormatResponse);
    }

    @Test
    void getById_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventFormatsService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenNameIsUnique_thenSaveWithTrimmedNameAndReturnResponse() {
        when(eventFormatRepository.existsByName("Liga")).thenReturn(false);
        when(eventFormatMapper.toEntity(createRequest)).thenReturn(eventFormatEntity);
        when(eventFormatRepository.save(eventFormatEntity)).thenReturn(eventFormatEntity);
        when(eventFormatMapper.toResponse(eventFormatEntity)).thenReturn(eventFormatResponse);

        EventFormatResponse result = eventFormatsService.create(createRequest);

        assertThat(result).isSameAs(eventFormatResponse);
        assertThat(eventFormatEntity.getName()).isEqualTo("Liga");
        verify(eventFormatRepository).save(eventFormatEntity);
    }

    @Test
    void create_whenNameAlreadyExists_thenThrowFieldValidationException() {
        when(eventFormatRepository.existsByName("Liga")).thenReturn(true);

        assertThatThrownBy(() -> eventFormatsService.create(createRequest))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsEntry("name", EventFormatError.NAME_ALREADY_EXISTS.name()));

        verify(eventFormatRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenFoundAndNameFree_thenReturnUpdatedResponse() {
        UUID id = eventFormatEntity.getId();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.of(eventFormatEntity));
        when(eventFormatRepository.existsByNameExcludingId("Eliminación directa", id)).thenReturn(false);
        when(eventFormatRepository.save(eventFormatEntity)).thenReturn(eventFormatEntity);
        when(eventFormatMapper.toResponse(eventFormatEntity)).thenReturn(eventFormatResponse);

        EventFormatResponse result = eventFormatsService.update(id, updateRequest);

        assertThat(result).isSameAs(eventFormatResponse);
        verify(eventFormatMapper).updateEntity(eventFormatEntity, updateRequest);
    }

    @Test
    void update_whenNameBelongsToAnotherFormat_thenThrowFieldValidationException() {
        UUID id = eventFormatEntity.getId();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.of(eventFormatEntity));
        when(eventFormatRepository.existsByNameExcludingId("Eliminación directa", id)).thenReturn(true);

        assertThatThrownBy(() -> eventFormatsService.update(id, updateRequest))
                .isInstanceOf(FieldValidationException.class);

        verify(eventFormatRepository, never()).save(any());
    }

    @Test
    void update_whenNameIsNotSent_thenSkipUniquenessCheck() {
        UUID id = eventFormatEntity.getId();
        updateRequest.setName(null);
        updateRequest.setDescription("Nueva descripción");
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.of(eventFormatEntity));
        when(eventFormatRepository.save(eventFormatEntity)).thenReturn(eventFormatEntity);
        when(eventFormatMapper.toResponse(eventFormatEntity)).thenReturn(eventFormatResponse);

        eventFormatsService.update(id, updateRequest);

        verify(eventFormatRepository, never()).existsByNameExcludingId(any(), any());
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventFormatsService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenNoEventUsesIt_thenMarkDeleted() {
        UUID id = eventFormatEntity.getId();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.of(eventFormatEntity));
        when(eventRepository.existsActiveByEventFormatId(id)).thenReturn(false);

        eventFormatsService.delete(id);

        assertThat(eventFormatEntity.getDeletedAt()).isNotNull();
        verify(eventFormatRepository).delete(eventFormatEntity);
    }

    @Test
    void delete_whenAnEventUsesIt_thenRefuseAndKeepTheFormat() {
        UUID id = eventFormatEntity.getId();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.of(eventFormatEntity));
        when(eventRepository.existsActiveByEventFormatId(id)).thenReturn(true);

        assertThatThrownBy(() -> eventFormatsService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(EventFormatError.IN_USE.name());

        assertThat(eventFormatEntity.getDeletedAt()).isNull();
        verify(eventFormatRepository, never()).delete(any());
    }

    @Test
    void delete_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventFormatRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventFormatsService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
