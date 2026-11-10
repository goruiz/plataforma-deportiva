package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.application.mappers.PlayerResponseAssembler;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

/** What the tests of {@link PlayersService} share. */
final class PlayersServiceTestSupport {

    private PlayersServiceTestSupport() {}

    /**
     * Gives the service the real rules for teams over the in-memory memberships: joining, leaving, the response with the
     * teams. {@code @InjectMocks} leaves those collaborators empty because no mock matches them.
     */
    static TeamMembershipsService useInMemoryMemberships(PlayersService service, PlayerMapper playerMapper,
                                                         InMemoryTeamMembers memberships,
                                                         IPlayerAccountOrganizationService accountOrganization) {
        TeamMembershipsService teamMemberships = new TeamMembershipsService(memberships, accountOrganization);
        ReflectionTestUtils.setField(service, "teamMemberRepository", memberships);
        ReflectionTestUtils.setField(service, "teamMembershipsService", teamMemberships);
        ReflectionTestUtils.setField(service, "playerResponses", new PlayerResponseAssembler(playerMapper, memberships));
        return teamMemberships;
    }

    static TeamsEntity team(String name, UUID organizationId) {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName(name);
        team.setOrganizationId(organizationId);
        return team;
    }

    static PlayersEntity player(String email) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail(email);
        return player;
    }
}
