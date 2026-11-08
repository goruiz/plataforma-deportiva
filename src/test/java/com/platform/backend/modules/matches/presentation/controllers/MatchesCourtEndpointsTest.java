package com.platform.backend.modules.matches.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.matches.application.iservices.ICourtAvailabilityService;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.presentation.responses.CourtAvailabilityResponse.CourtAvailabilityResponse;
import com.platform.backend.shared.infraestructure.exceptions.GlobalException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The court endpoints with the real {@code GlobalException} in front: what a client gets for a missing or unreadable
 * parameter and for a court it cannot see.
 */
@ExtendWith(MockitoExtension.class)
class MatchesCourtEndpointsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private MockMvc mockMvc;
    private final UUID courtId = UUID.randomUUID();

    @Mock private ICourtAvailabilityService courtAvailabilityService;
    @Mock private IMatchesService matchesService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CourtAvailabilityController(courtAvailabilityService), new MatchesController(matchesService))
                .setControllerAdvice(new GlobalException())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getAvailability_whenTheDateIsSent_thenTheWholeAvailabilityIsReturned() throws Exception {
        when(courtAvailabilityService.getAvailability(courtId, DAY)).thenReturn(new CourtAvailabilityResponse(
                courtId, DAY, LocalTime.of(8, 0), LocalTime.of(22, 0), List.of(), List.of()));

        mockMvc.perform(get("/courts/{id}/availability", courtId).param("date", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courtId").value(courtId.toString()))
                .andExpect(jsonPath("$.data.date").value("2026-11-10"))
                .andExpect(jsonPath("$.data.opensAt").value("08:00:00"))
                .andExpect(jsonPath("$.data.closesAt").value("22:00:00"));
    }

    @Test
    void getAvailability_whenTheDateIsMissing_thenBadRequestNamingTheParameter() throws Exception {
        mockMvc.perform(get("/courts/{id}/availability", courtId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.date").value("PARAMETER_REQUIRED"));

        verifyNoInteractions(courtAvailabilityService);
    }

    @Test
    void getAvailability_whenTheDateIsNotADate_thenBadRequestNamingTheParameter() throws Exception {
        mockMvc.perform(get("/courts/{id}/availability", courtId).param("date", "10/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.date").value("PARAMETER_INVALID"));
    }

    @Test
    void getAvailability_whenTheCourtIsNotVisible_thenNotFound() throws Exception {
        when(courtAvailabilityService.getAvailability(courtId, DAY)).thenThrow(new EntityNotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{id}/availability", courtId).param("date", "2026-11-10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByCourt_whenFromIsSent_thenItIsPassedOnAsADateTime() throws Exception {
        when(matchesService.getByCourtId(courtId, LocalDateTime.of(2026, 11, 10, 0, 0))).thenReturn(List.of());

        mockMvc.perform(get("/matches/by-court/{id}", courtId).param("from", "2026-11-10T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(matchesService).getByCourtId(courtId, LocalDateTime.of(2026, 11, 10, 0, 0));
    }

    @Test
    void getByCourt_whenFromIsOmitted_thenAllTheMatchesOfTheCourtAreAskedFor() throws Exception {
        when(matchesService.getByCourtId(courtId, null)).thenReturn(List.of());

        mockMvc.perform(get("/matches/by-court/{id}", courtId)).andExpect(status().isOk());

        verify(matchesService).getByCourtId(courtId, null);
    }

    @Test
    void getByCourt_whenFromIsNotADateTime_thenBadRequestNamingTheParameter() throws Exception {
        mockMvc.perform(get("/matches/by-court/{id}", courtId).param("from", "tomorrow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.from").value("PARAMETER_INVALID"));
    }

    @Test
    void getByCourt_whenTheCourtIdIsNotAUuid_thenBadRequestNamingThePathVariable() throws Exception {
        mockMvc.perform(get("/matches/by-court/{id}", "cancha-uno"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.courtId").value("PARAMETER_INVALID"));
    }

    @Test
    void handleUploadTooLarge_whenAnUploadExceedsTheLimit_thenBadRequestOnTheFilePart() {
        var response = new GlobalException().handleUploadTooLarge(new MaxUploadSizeExceededException(2_097_152L));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData()).containsEntry("file", "FILE_TOO_LARGE");
    }
}
