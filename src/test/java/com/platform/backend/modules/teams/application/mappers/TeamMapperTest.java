package com.platform.backend.modules.teams.application.mappers;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.presentation.responses.TeamResponse.TeamResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMapperTest {

    private final TeamMapper mapper = Mappers.getMapper(TeamMapper.class);

    @Test
    void toResponse_whenTeamBelongsToAnOrganization_thenExposeIt() {
        UUID organizationId = UUID.randomUUID();
        TeamsEntity team = new TeamsEntity();
        team.setName("Los Tigres");
        team.setOrganizationId(organizationId);

        TeamResponse response = mapper.toResponse(team);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void toResponse_whenTeamIsThePlatformOwners_thenOrganizationIsNull() {
        TeamsEntity team = new TeamsEntity();
        team.setName("Equipo del propietario");

        TeamResponse response = mapper.toResponse(team);

        assertThat(response.getOrganizationId()).isNull();
    }
}
