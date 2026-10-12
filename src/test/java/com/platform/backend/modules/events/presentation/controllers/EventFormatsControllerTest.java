package com.platform.backend.modules.events.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.events.application.iservices.IEventFormatsService;
import com.platform.backend.modules.events.domain.exception.EventFormatError;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.exceptions.GlobalException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class EventFormatsControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private IEventFormatsService eventFormatsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventFormatsController(eventFormatsService))
                .setControllerAdvice(new GlobalException())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ─── GET /event-formats ─────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturn200WithList() throws Exception {
        when(eventFormatsService.getAll()).thenReturn(List.of(buildResponse("Liga")));

        mockMvc.perform(get("/event-formats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Liga"));
    }

    // ─── GET /event-formats/{id} ────────────────────────────────────────────────

    @Test
    void getById_whenFound_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventFormatsService.getById(id)).thenReturn(buildResponse("Liga"));

        mockMvc.perform(get("/event-formats/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Liga"));
    }

    @Test
    void getById_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventFormatsService.getById(id)).thenThrow(new EntityNotFoundException("Event format not found with id: " + id));

        mockMvc.perform(get("/event-formats/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /event-formats ────────────────────────────────────────────────────

    @Test
    void create_whenValidBody_thenReturn201() throws Exception {
        when(eventFormatsService.create(any())).thenReturn(buildResponse("Liga"));

        mockMvc.perform(post("/event-formats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Liga"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Liga"));
    }

    @Test
    void create_whenNameIsBlank_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/event-formats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void create_whenNameAlreadyExists_thenReturn400WithReasonCodeOnNameField() throws Exception {
        when(eventFormatsService.create(any()))
                .thenThrow(new FieldValidationException("name", EventFormatError.NAME_ALREADY_EXISTS.name()));

        mockMvc.perform(post("/event-formats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Liga"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").value(EventFormatError.NAME_ALREADY_EXISTS.name()));
    }

    // ─── PUT /event-formats/{id} ────────────────────────────────────────────────

    @Test
    void update_whenValidBody_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventFormatsService.update(eq(id), any())).thenReturn(buildResponse("Eliminación directa"));

        UpdateEventFormatRequest request = new UpdateEventFormatRequest();
        request.setName("Eliminación directa");

        mockMvc.perform(put("/event-formats/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Eliminación directa"));
    }

    @Test
    void update_whenNameIsBlank_thenReturn400() throws Exception {
        UpdateEventFormatRequest request = new UpdateEventFormatRequest();
        request.setName("   ");

        mockMvc.perform(put("/event-formats/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void update_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventFormatsService.update(eq(id), any())).thenThrow(new EntityNotFoundException("Event format not found with id: " + id));

        mockMvc.perform(put("/event-formats/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /event-formats/{id} ─────────────────────────────────────────────

    @Test
    void delete_whenFound_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(eventFormatsService).delete(id);

        mockMvc.perform(delete("/event-formats/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }

    @Test
    void delete_whenFormatIsInUse_thenReturn400WithReasonCode() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalStateException(EventFormatError.IN_USE.name())).when(eventFormatsService).delete(id);

        mockMvc.perform(delete("/event-formats/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EventFormatError.IN_USE.name()));
    }

    @Test
    void delete_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Event format not found with id: " + id)).when(eventFormatsService).delete(id);

        mockMvc.perform(delete("/event-formats/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private EventFormatResponse buildResponse(String name) {
        EventFormatResponse response = new EventFormatResponse();
        response.setId(UUID.randomUUID());
        response.setName(name);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private CreateEventFormatRequest buildCreateRequest(String name) {
        CreateEventFormatRequest request = new CreateEventFormatRequest();
        request.setName(name);
        return request;
    }
}
