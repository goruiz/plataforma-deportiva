package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.application.mappers.EventTypeMapper;
import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventTypeRequest.CreateEventTypeRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventTypeRequest.UpdateEventTypeRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventTypesServiceTest {

    @Mock private IEventTypeRepository eventTypeRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private EventTypeMapper eventTypeMapper;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventTypesService eventTypesService;

    private EventTypesEntity eventTypeEntity;
    private EventTypeResponse eventTypeResponse;
    private CreateEventTypeRequest createRequest;
    private UpdateEventTypeRequest updateRequest;

    @BeforeEach
    void setUp() {
        eventTypeEntity = new EventTypesEntity();
        eventTypeEntity.setId(UUID.randomUUID());
        eventTypeEntity.setName("Football");
        eventTypeEntity.setDescription("Football type");

        eventTypeResponse = new EventTypeResponse();
        eventTypeResponse.setId(UUID.randomUUID());
        eventTypeResponse.setName("Football");
        eventTypeResponse.setDescription("Football type");

        createRequest = new CreateEventTypeRequest();
        createRequest.setName("Football");
        createRequest.setDescription("Football type");

        updateRequest = new UpdateEventTypeRequest();
        updateRequest.setName("Basketball");
    }

    // ─── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturnMappedList() {
        when(eventTypeRepository.findAllActive()).thenReturn(List.of(eventTypeEntity));
        when(eventTypeMapper.toResponse(eventTypeEntity)).thenReturn(eventTypeResponse);

        List<EventTypeResponse> result = eventTypesService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Football");
        verify(eventTypeRepository).findAllActive();
        verify(eventTypeMapper).toResponse(eventTypeEntity);
    }

    // ─── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_whenFound_thenReturnMappedResponse() {
        UUID id = eventTypeEntity.getId();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.of(eventTypeEntity));
        when(eventTypeMapper.toResponse(eventTypeEntity)).thenReturn(eventTypeResponse);

        EventTypeResponse result = eventTypesService.getById(id);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Football");
        verify(eventTypeRepository).findActiveById(id);
    }

    @Test
    void getById_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypesService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── getEventsByType ────────────────────────────────────────────────────────

    @Test
    void getEventsByType_whenTypeFound_thenReturnMappedEvents() {
        UUID id = eventTypeEntity.getId();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.of(eventTypeEntity));
        when(eventRepository.findAllActiveByEventTypeId(id)).thenReturn(List.of());

        List<EventResponse> result = eventTypesService.getEventsByType(id);

        assertThat(result).isNotNull();
        verify(eventTypeRepository).findActiveById(id);
        verify(eventRepository).findAllActiveByEventTypeId(id);
    }

    @Test
    void getEventsByType_whenTypeNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypesService.getEventsByType(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_whenNameIsUnique_thenReturnCreatedResponse() {
        when(eventTypeRepository.existsByName("Football")).thenReturn(false);
        when(eventTypeMapper.toEntity(createRequest)).thenReturn(eventTypeEntity);
        when(eventTypeRepository.save(eventTypeEntity)).thenReturn(eventTypeEntity);
        when(eventTypeMapper.toResponse(eventTypeEntity)).thenReturn(eventTypeResponse);

        EventTypeResponse result = eventTypesService.create(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Football");
        verify(eventTypeRepository).save(eventTypeEntity);
    }

    @Test
    void create_whenNameAlreadyExists_thenThrowDataIntegrityViolationException() {
        when(eventTypeRepository.existsByName("Football")).thenReturn(true);

        assertThatThrownBy(() -> eventTypesService.create(createRequest))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(eventTypeRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_thenReturnUpdatedResponse() {
        UUID id = eventTypeEntity.getId();
        EventTypeResponse updatedResponse = new EventTypeResponse();
        updatedResponse.setName("Basketball");
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.of(eventTypeEntity));
        when(eventTypeRepository.save(eventTypeEntity)).thenReturn(eventTypeEntity);
        when(eventTypeMapper.toResponse(eventTypeEntity)).thenReturn(updatedResponse);

        EventTypeResponse result = eventTypesService.update(id, updateRequest);

        assertThat(result.getName()).isEqualTo("Basketball");
        verify(eventTypeMapper).updateEntity(eventTypeEntity, updateRequest);
        verify(eventTypeRepository).save(eventTypeEntity);
    }

    @Test
    void update_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypesService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_thenSetsDeletedAtAndCallsDelete() {
        UUID id = eventTypeEntity.getId();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.of(eventTypeEntity));
        doNothing().when(eventTypeRepository).delete(eventTypeEntity);

        eventTypesService.delete(id);

        assertThat(eventTypeEntity.getDeletedAt()).isNotNull();
        verify(eventTypeRepository).delete(eventTypeEntity);
    }

    @Test
    void delete_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypesService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── hardDelete ─────────────────────────────────────────────────────────────

    @Test
    void hardDelete_whenFound_thenCallsHardDelete() {
        UUID id = eventTypeEntity.getId();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventTypeEntity));
        doNothing().when(eventTypeRepository).hardDelete(eventTypeEntity);

        eventTypesService.hardDelete(id);

        verify(eventTypeRepository).hardDelete(eventTypeEntity);
    }

    @Test
    void hardDelete_whenNotFound_thenThrowEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypesService.hardDelete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
