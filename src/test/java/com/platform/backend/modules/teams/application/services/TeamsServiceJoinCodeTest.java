package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.teams.application.mappers.TeamMapper;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.presentation.responses.TeamJoinCodeResponse.TeamJoinCodeResponse;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The code a team shares with its players: only whoever may change the team can read, replace or switch it off. */
@ExtendWith(MockitoExtension.class)
class TeamsServiceJoinCodeTest {

    @Mock private ITeamRepository teamRepository;
    @Mock private ICategoryRepository categoryRepository;
    @Mock private TeamMapper teamMapper;
    @Mock private JoinCodeGenerator joinCodeGenerator;

    @InjectMocks
    private TeamsService teamsService;

    private TeamsEntity team;

    @BeforeEach
    void setUp() {
        team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName("Tigres");
        lenient().when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    // ─── generateJoinCode ───────────────────────────────────────────────────────

    @Test
    void generateJoinCode_whenTheTeamHasNone_thenItGetsTheDrawnCode() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        when(joinCodeGenerator.generate()).thenReturn("AB3D9FK2");
        when(teamRepository.existsActiveByJoinCode("AB3D9FK2")).thenReturn(false);
        when(teamRepository.save(team)).thenReturn(team);

        TeamJoinCodeResponse response = teamsService.generateJoinCode(team.getId());

        assertThat(response.getTeamId()).isEqualTo(team.getId());
        assertThat(response.getJoinCode()).isEqualTo("AB3D9FK2");
        assertThat(team.getJoinCode()).isEqualTo("AB3D9FK2");
    }

    @Test
    void generateJoinCode_whenTheTeamHadACode_thenTheOldOneIsReplaced() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        team.setJoinCode("OLDCODE1");
        when(joinCodeGenerator.generate()).thenReturn("NEWCODE2");
        when(teamRepository.save(team)).thenReturn(team);

        teamsService.generateJoinCode(team.getId());

        assertThat(team.getJoinCode()).isEqualTo("NEWCODE2");
    }

    @Test
    void generateJoinCode_whenTheDrawnCodeIsAlreadyTaken_thenAnotherIsDrawn() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        when(joinCodeGenerator.generate()).thenReturn("TAKEN001", "FREE0002");
        when(teamRepository.existsActiveByJoinCode("TAKEN001")).thenReturn(true);
        when(teamRepository.existsActiveByJoinCode("FREE0002")).thenReturn(false);
        when(teamRepository.save(team)).thenReturn(team);

        teamsService.generateJoinCode(team.getId());

        assertThat(team.getJoinCode()).isEqualTo("FREE0002");
    }

    @Test
    void generateJoinCode_whenEveryDrawnCodeIsTaken_thenItGivesUpWithoutChangingTheTeam() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        when(joinCodeGenerator.generate()).thenReturn("TAKEN001");
        when(teamRepository.existsActiveByJoinCode("TAKEN001")).thenReturn(true);

        assertThatThrownBy(() -> teamsService.generateJoinCode(team.getId()))
                .isInstanceOf(IllegalStateException.class);

        verify(joinCodeGenerator, times(5)).generate();
        verify(teamRepository, never()).save(any());
        assertThat(team.getJoinCode()).isNull();
    }

    // ─── disableJoinCode and getJoinCode ────────────────────────────────────────

    @Test
    void disableJoinCode_whenTheTeamHadACode_thenNobodyCanJoinWithItAnyMore() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        team.setJoinCode("AB3D9FK2");
        when(teamRepository.save(team)).thenReturn(team);

        TeamJoinCodeResponse response = teamsService.disableJoinCode(team.getId());

        assertThat(response.getJoinCode()).isNull();
        assertThat(team.getJoinCode()).isNull();
    }

    @Test
    void getJoinCode_whenTheCallerMayChangeTheTeam_thenItIsRead() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        team.setJoinCode("AB3D9FK2");

        assertThat(teamsService.getJoinCode(team.getId()).getJoinCode()).isEqualTo("AB3D9FK2");
    }

    @Test
    void getJoinCode_whenTheTeamAdmitsNoCodes_thenTheAnswerHasNoCode() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());

        assertThat(teamsService.getJoinCode(team.getId()).getJoinCode()).isNull();
    }

    @Test
    void getJoinCode_whenTheCallerIsAnotherPlayerWhoDidNotCreateTheTeam_thenItIsForbidden() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), "juan@email.com");

        assertThatThrownBy(() -> teamsService.getJoinCode(team.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessError.NOT_THE_CREATOR.name());
    }

    @Test
    void generateJoinCode_whenTheCallerIsAnotherPlayerWhoDidNotCreateTheTeam_thenNoCodeIsDrawn() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), "juan@email.com");

        assertThatThrownBy(() -> teamsService.generateJoinCode(team.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(joinCodeGenerator, never()).generate();
    }

    @Test
    void generateJoinCode_whenTheTeamDoesNotExist_thenItIsNotFound() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        UUID unknownId = UUID.randomUUID();
        when(teamRepository.findActiveById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamsService.generateJoinCode(unknownId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
