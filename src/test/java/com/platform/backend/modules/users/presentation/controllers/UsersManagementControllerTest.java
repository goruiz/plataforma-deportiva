package com.platform.backend.modules.users.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.backend.modules.users.application.iservices.IUsersManagementService;
import com.platform.backend.modules.users.domain.enums.UserSection;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UserListItemResponse.UserListItemResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.infraestructure.exceptions.GlobalException;
import com.platform.backend.shared.infraestructure.pagination.ListQueryResolver;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The paginated listings of {@code /users}: how their query string is read and refused. Permissions are not part of this standalone setup. */
@ExtendWith(MockitoExtension.class)
class UsersManagementControllerTest {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Mock
    private IUsersManagementService usersManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsersManagementController(usersManagementService, new ListQueryResolver(DEFAULT_SIZE, MAX_SIZE, 100, 5)))
                .setControllerAdvice(new GlobalException())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private PageResponse<UserListItemResponse> aPageWithOneAccount(PageQuery query, long total) {
        UserListItemResponse account = new UserListItemResponse();
        account.setId(UUID.randomUUID());
        account.setEmail("ana@email.com");
        account.setRoles(List.of());
        return new PageResponse<>(List.of(account), query.page(), query.size(), total, 1);
    }

    // ─── each tab asks for its own section ──────────────────────────────────────

    @Test
    void getAdminUsers_whenTheWholeQueryStringIsSent_thenThePageSizeAndWordsReachTheService() throws Exception {
        PageQuery query = new PageQuery(2, 10);
        when(usersManagementService.getUsers(UserSection.ADMINS, query, List.of("ana", "perez")))
                .thenReturn(aPageWithOneAccount(query, 41));

        mockMvc.perform(get("/users/admins").param("page", "2").param("size", "10").param("search", "  Ana PEREZ "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("ana@email.com"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(41));
    }

    @Test
    void listings_whenNoParametersAreSent_thenTheyAskForTheFirstPageOfTheDefaultSizeWithoutSearch() throws Exception {
        PageQuery firstPage = new PageQuery(0, DEFAULT_SIZE);
        when(usersManagementService.getUsers(any(), any(), any())).thenReturn(aPageWithOneAccount(firstPage, 1));

        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(get("/users/owners")).andExpect(status().isOk());
        mockMvc.perform(get("/users/regular")).andExpect(status().isOk());

        verify(usersManagementService).getUsers(UserSection.ALL, firstPage, List.of());
        verify(usersManagementService).getUsers(UserSection.OWNERS, firstPage, List.of());
        verify(usersManagementService).getUsers(UserSection.APP_USERS, firstPage, List.of());
    }

    @Test
    void counts_whenSearching_thenOnlyTheWordsAreRead() throws Exception {
        when(usersManagementService.countUsersBySection(List.of("ana"))).thenReturn(new UserCountsResponse(1, 2, 3));

        mockMvc.perform(get("/users/counts").param("search", "ANA").param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owners").value(1))
                .andExpect(jsonPath("$.data.admins").value(2))
                .andExpect(jsonPath("$.data.appUsers").value(3));
    }

    // ─── what is refused ────────────────────────────────────────────────────────

    @Test
    void listings_whenTheSizeIsAboveTheMaximum_thenItIsA400OnTheSizeFieldAndNothingIsRead() throws Exception {
        mockMvc.perform(get("/users/regular").param("size", String.valueOf(MAX_SIZE + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.size").value("SIZE_OUT_OF_RANGE"));

        verifyNoInteractions(usersManagementService);
    }

    @Test
    void listings_whenTheSizeIsZero_thenItIsA400OnTheSizeField() throws Exception {
        mockMvc.perform(get("/users/admins").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.size").value("SIZE_OUT_OF_RANGE"));
    }

    @Test
    void listings_whenThePageIsNegative_thenItIsA400OnThePageField() throws Exception {
        mockMvc.perform(get("/users/admins").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.page").value("PAGE_OUT_OF_RANGE"));

        verifyNoInteractions(usersManagementService);
    }

    @Test
    void listings_whenThePageIsNotANumber_thenItIsA400OnThePageField() throws Exception {
        mockMvc.perform(get("/users/admins").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.page").exists());

        verifyNoInteractions(usersManagementService);
    }

    @Test
    void listings_whenTheSearchIsTooLong_thenItIsA400OnTheSearchField() throws Exception {
        mockMvc.perform(get("/users").param("search", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.search").value("SEARCH_TOO_LONG"));

        verifyNoInteractions(usersManagementService);
    }
}
