package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.teams.application.mappers.TeamsEventsMapper;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.domain.enums.TeamStatus;
import com.platform.backend.modules.teams.domain.exception.TeamError;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamsEventsRequest.CreateTeamsEventsRequest;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Enrolling a team in an event: an inactive team receives nothing new, so it is not enrolled. */
@ExtendWith(MockitoExtension.class)
class TeamsEventsServiceTest {

    @Mock private ITeamsEventsRepository teamsEventsRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private TeamsEventsMapper teamsEventsMapper;

    @InjectMocks
    private TeamsEventsService teamsEventsService;

    private TeamsEntity team;
    private EventsEntity event;
    private CreateTeamsEventsRequest request;

    @BeforeEach
    void setUp() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID());
        team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName("Tigres");
        event = new EventsEntity();
        event.setId(UUID.randomUUID());
        request = new CreateTeamsEventsRequest();
        request.setTeamId(team.getId());
        request.setEventId(event.getId());
        when(teamRepository.findActiveById(team.getId())).thenReturn(Optional.of(team));
        when(eventRepository.findActiveById(event.getId())).thenReturn(Optional.of(event));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    @Test
    void create_whenTheTeamIsActive_thenItIsEnrolled() {
        when(teamsEventsRepository.save(any(TeamsEventsEntity.class))).thenAnswer(call -> call.getArgument(0));

        teamsEventsService.create(request);

        verify(teamsEventsRepository).save(any(TeamsEventsEntity.class));
    }

    @Test
    void create_whenTheTeamIsInactive_thenItIsRefusedWithTheCodeAndNothingIsSaved() {
        team.setStatus(TeamStatus.INACTIVE.name());

        assertThatThrownBy(() -> teamsEventsService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TeamError.TEAM_INACTIVE.name());

        verify(teamsEventsRepository, never()).save(any());
    }
}
