package com.platform.backend.modules.holidays.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.holidays.application.iservices.IHolidaysService;
import com.platform.backend.modules.holidays.domain.exception.HolidayError;
import com.platform.backend.modules.holidays.presentation.requests.CreateHolidayRequest.CreateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.requests.UpdateHolidayRequest.UpdateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.responses.HolidayResponse.HolidayResponse;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HolidaysControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private IHolidaysService holidaysService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new HolidaysController(holidaysService))
                .setControllerAdvice(new GlobalException())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private static HolidayResponse buildResponse(String name, LocalDate date, boolean recurring) {
        HolidayResponse response = new HolidayResponse();
        response.setId(UUID.randomUUID());
        response.setName(name);
        response.setDate(date);
        response.setRecurring(recurring);
        return response;
    }

    private static CreateHolidayRequest buildCreateRequest(String name, LocalDate date) {
        CreateHolidayRequest request = new CreateHolidayRequest();
        request.setName(name);
        request.setDate(date);
        return request;
    }

    // ─── GET /holidays ──────────────────────────────────────────────────────────

    @Test
    void getAll_whenCalled_thenReturn200WithList() throws Exception {
        when(holidaysService.getAll()).thenReturn(List.of(buildResponse("Navidad", LocalDate.of(2026, 12, 25), true)));

        mockMvc.perform(get("/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Navidad"))
                .andExpect(jsonPath("$.data[0].date").value("2026-12-25"))
                .andExpect(jsonPath("$.data[0].recurring").value(true));
    }

    @Test
    void getById_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(holidaysService.getById(id)).thenThrow(new EntityNotFoundException("Holiday not found with id: " + id));

        mockMvc.perform(get("/holidays/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /holidays ─────────────────────────────────────────────────────────

    @Test
    void create_whenValidBody_thenReturn201() throws Exception {
        when(holidaysService.create(any())).thenReturn(buildResponse("Carnaval", LocalDate.of(2027, 2, 15), false));

        mockMvc.perform(post("/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Carnaval", LocalDate.of(2027, 2, 15)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Carnaval"));
    }

    @Test
    void create_whenNameIsBlank_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("  ", LocalDate.of(2027, 2, 15)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void create_whenDateIsMissing_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Carnaval", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.date").exists());
    }

    @Test
    void create_whenNameIsTooLong_thenReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("x".repeat(101), LocalDate.of(2027, 2, 15)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void create_whenDateAlreadyCovered_thenReturn400WithReasonCodeOnDateField() throws Exception {
        when(holidaysService.create(any()))
                .thenThrow(new FieldValidationException("date", HolidayError.DATE_ALREADY_EXISTS.name()));

        mockMvc.perform(post("/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Navidad", LocalDate.of(2026, 12, 25)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.date").value("DATE_ALREADY_EXISTS"));
    }

    // ─── PUT /holidays/{id} ─────────────────────────────────────────────────────

    @Test
    void update_whenValidBody_thenReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setName("Navidad del año");
        when(holidaysService.update(eq(id), any())).thenReturn(buildResponse("Navidad del año", LocalDate.of(2026, 12, 25), true));

        mockMvc.perform(put("/holidays/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Navidad del año"));
    }

    @Test
    void update_whenNameIsBlank_thenReturn400WithFieldError() throws Exception {
        UpdateHolidayRequest request = new UpdateHolidayRequest();
        request.setName("   ");

        mockMvc.perform(put("/holidays/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists());
    }

    // ─── DELETE /holidays/{id} ──────────────────────────────────────────────────

    @Test
    void delete_whenFound_thenReturn200() throws Exception {
        mockMvc.perform(delete("/holidays/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_whenNotFound_thenReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Holiday not found with id: " + id)).when(holidaysService).delete(id);

        mockMvc.perform(delete("/holidays/{id}", id))
                .andExpect(status().isNotFound());
    }
}
