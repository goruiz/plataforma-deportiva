package com.platform.backend.modules.events.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.events.application.iservices.ICategoriesService;
import com.platform.backend.modules.events.domain.exception.CategoryError;
import com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest.CreateCategoryRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest.UpdateCategoryRequest;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;
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
class CategoriesControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ICategoriesService categoriesService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CategoriesController(categoriesService))
                .setControllerAdvice(new GlobalException())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ─── GET /categories ────────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturn200WithList() throws Exception {
        when(categoriesService.getAll()).thenReturn(List.of(buildResponse("Sub-15")));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Sub-15"));
    }

    // ─── GET /categories/{id} ───────────────────────────────────────────────────

    @Test
    void getById_whenFound_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriesService.getById(id)).thenReturn(buildResponse("Sub-15"));

        mockMvc.perform(get("/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sub-15"))
                .andExpect(jsonPath("$.data.description").value("Hasta 15 años"));
    }

    @Test
    void getById_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriesService.getById(id)).thenThrow(new EntityNotFoundException("Category not found with id: " + id));

        mockMvc.perform(get("/categories/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /categories ───────────────────────────────────────────────────────

    @Test
    void create_whenValidBody_thenReturn201() throws Exception {
        when(categoriesService.create(any())).thenReturn(buildResponse("Sub-15"));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Sub-15"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Sub-15"));
    }

    @Test
    void create_whenNameIsBlank_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void create_whenDescriptionIsTooLong_thenReturn400WithFieldError() throws Exception {
        CreateCategoryRequest request = buildCreateRequest("Sub-15");
        request.setDescription("x".repeat(256));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.description").exists());
    }

    @Test
    void create_whenNameAlreadyExists_thenReturn400WithReasonCodeOnNameField() throws Exception {
        when(categoriesService.create(any()))
                .thenThrow(new FieldValidationException("name", CategoryError.NAME_ALREADY_EXISTS.name()));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Sub-15"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").value(CategoryError.NAME_ALREADY_EXISTS.name()));
    }

    // ─── PUT /categories/{id} ───────────────────────────────────────────────────

    @Test
    void update_whenValidBody_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriesService.update(eq(id), any())).thenReturn(buildResponse("Sub-17"));

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Sub-17");

        mockMvc.perform(put("/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sub-17"));
    }

    @Test
    void update_whenNameIsBlank_thenReturn400() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("   ");

        mockMvc.perform(put("/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void update_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriesService.update(eq(id), any())).thenThrow(new EntityNotFoundException("Category not found with id: " + id));

        mockMvc.perform(put("/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /categories/{id} ────────────────────────────────────────────────

    @Test
    void delete_whenFound_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(categoriesService).delete(id);

        mockMvc.perform(delete("/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }

    @Test
    void delete_whenCategoryIsInUse_thenReturn400WithReasonCode() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalStateException(CategoryError.IN_USE.name())).when(categoriesService).delete(id);

        mockMvc.perform(delete("/categories/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CategoryError.IN_USE.name()));
    }

    @Test
    void delete_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Category not found with id: " + id)).when(categoriesService).delete(id);

        mockMvc.perform(delete("/categories/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private CategoryResponse buildResponse(String name) {
        CategoryResponse response = new CategoryResponse();
        response.setId(UUID.randomUUID());
        response.setName(name);
        response.setDescription("Hasta 15 años");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private CreateCategoryRequest buildCreateRequest(String name) {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName(name);
        return request;
    }
}
