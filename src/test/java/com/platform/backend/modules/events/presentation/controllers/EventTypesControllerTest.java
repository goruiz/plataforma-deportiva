package com.platform.backend.modules.events.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.events.application.iservices.IEventTypesService;
import com.platform.backend.modules.events.presentation.requests.CreateEventTypeRequest.CreateEventTypeRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventTypeRequest.UpdateEventTypeRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;
import com.platform.backend.shared.infraestructure.exceptions.GlobalException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventTypesControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private IEventTypesService eventTypesService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventTypesController(eventTypesService))
                .setControllerAdvice(new GlobalException())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ─── GET /event-types ───────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturn200WithList() throws Exception {
        when(eventTypesService.getAll()).thenReturn(List.of(buildEventTypeResponse("Football", "Football type")));

        mockMvc.perform(get("/event-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Football"));
    }

    // ─── GET /event-types/{id} ──────────────────────────────────────────────────

    @Test
    void getById_whenFound_thenReturn200WithEventType() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventTypesService.getById(id)).thenReturn(buildEventTypeResponse("Football", "Football type"));

        mockMvc.perform(get("/event-types/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Football"));
    }

    @Test
    void getById_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventTypesService.getById(id)).thenThrow(new EntityNotFoundException("Event type not found with id: " + id));

        mockMvc.perform(get("/event-types/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── GET /event-types/{id}/events ───────────────────────────────────────────

    @Test
    void getEventsByType_whenTypeFound_thenReturn200WithEvents() throws Exception {
        UUID id = UUID.randomUUID();
        EventResponse eventResponse = new EventResponse();
        eventResponse.setName("Championship 2026");
        when(eventTypesService.getEventsByType(id)).thenReturn(List.of(eventResponse));

        mockMvc.perform(get("/event-types/{id}/events", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Championship 2026"));
    }

    @Test
    void getEventsByType_whenTypeNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventTypesService.getEventsByType(id)).thenThrow(new EntityNotFoundException("Event type not found with id: " + id));

        mockMvc.perform(get("/event-types/{id}/events", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /event-types ──────────────────────────────────────────────────────

    @Test
    void create_whenValidBody_thenReturn201WithEventType() throws Exception {
        when(eventTypesService.create(any())).thenReturn(buildEventTypeResponse("Football", "Football type"));

        mockMvc.perform(post("/event-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Football", "Football type"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Football"));
    }

    @Test
    void create_whenNameIsBlank_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/event-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_whenMissingRequiredFields_thenReturn400() throws Exception {
        mockMvc.perform(post("/event-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    void create_whenNameAlreadyExists_thenReturn409() throws Exception {
        when(eventTypesService.create(any())).thenThrow(new DataIntegrityViolationException("Event type name already exists"));

        mockMvc.perform(post("/event-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Football", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── PUT /event-types/{id} ──────────────────────────────────────────────────

    @Test
    void update_whenValidBody_thenReturn200WithUpdatedEventType() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventTypesService.update(eq(id), any())).thenReturn(buildEventTypeResponse("Basketball", "Updated desc"));

        UpdateEventTypeRequest request = new UpdateEventTypeRequest();
        request.setName("Basketball");

        mockMvc.perform(put("/event-types/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Basketball"));
    }

    @Test
    void update_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventTypesService.update(eq(id), any())).thenThrow(new EntityNotFoundException("Event type not found with id: " + id));

        UpdateEventTypeRequest request = new UpdateEventTypeRequest();
        request.setName("Basketball");

        mockMvc.perform(put("/event-types/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── DELETE /event-types/{id} ───────────────────────────────────────────────

    @Test
    void delete_whenFound_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(eventTypesService).delete(id);

        mockMvc.perform(delete("/event-types/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }

    @Test
    void delete_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Event type not found with id: " + id)).when(eventTypesService).delete(id);

        mockMvc.perform(delete("/event-types/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── DELETE /event-types/{id}/hard ──────────────────────────────────────────

    @Test
    void hardDelete_whenFound_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(eventTypesService).hardDelete(id);

        mockMvc.perform(delete("/event-types/{id}/hard", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }

    @Test
    void hardDelete_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Event type not found with id: " + id)).when(eventTypesService).hardDelete(id);

        mockMvc.perform(delete("/event-types/{id}/hard", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private EventTypeResponse buildEventTypeResponse(String name, String description) {
        EventTypeResponse response = new EventTypeResponse();
        response.setId(UUID.randomUUID());
        response.setName(name);
        response.setDescription(description);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private CreateEventTypeRequest buildCreateRequest(String name, String description) {
        CreateEventTypeRequest request = new CreateEventTypeRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}
