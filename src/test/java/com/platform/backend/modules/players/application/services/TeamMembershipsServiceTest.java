package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.enums.TeamStatus;
import com.platform.backend.modules.teams.domain.exception.TeamError;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.player;
import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** A player plays for several teams, but only one per organization. */
@ExtendWith(MockitoExtension.class)
class TeamMembershipsServiceTest {

    private static final String EMAIL = "ana@email.com";
    private static final UUID ORGANIZATION_A = UUID.randomUUID();
    private static final UUID ORGANIZATION_B = UUID.randomUUID();

    @Mock private IPlayerAccountOrganizationService accountOrganization;

    private InMemoryTeamMembers memberships;
    private TeamMembershipsService service;
    private PlayersEntity ana;

    @BeforeEach
    void setUp() {
        memberships = new InMemoryTeamMembers();
        service = new TeamMembershipsService(memberships, accountOrganization, PlayersServiceTestSupport.categoriesWithoutRules());
        ana = player(EMAIL);
    }

    @Test
    void join_whenThePlayerHasNoTeam_thenTheyPlayForIt() {
        TeamsEntity tigres = team("Tigres", ORGANIZATION_A);

        TeamMembersEntity membership = service.join(ana, tigres);

        assertThat(memberships.activeTeamsOf(ana)).containsExactly(tigres);
        assertThat(membership.getOrganizationId()).isEqualTo(ORGANIZATION_A);
        assertThat(membership.getIsCaptain()).as("a new member is not the captain").isFalse();
        verify(accountOrganization).followTeam(EMAIL, ORGANIZATION_A);
    }

    @Test
    void join_whenThePlayerAlreadyPlaysForATeamOfThatOrganization_thenTheNewOneReplacesItAndTheOldOneIsKeptAsHistory() {
        TeamsEntity tigres = team("Tigres", ORGANIZATION_A);
        TeamsEntity leones = team("Leones", ORGANIZATION_A);
        TeamMembersEntity inTigres = service.join(ana, tigres);
        inTigres.setIsCaptain(true);

        service.join(ana, leones);

        assertThat(memberships.activeTeamsOf(ana)).containsExactly(leones);
        assertThat(inTigres.getDeletedAt()).isNotNull();
        assertThat(inTigres.getIsCaptain()).as("the captaincy ends with the membership").isFalse();
        assertThat(memberships.everyMembershipOf(ana)).hasSize(2);
    }

    @Test
    void join_whenThePlayerPlaysForATeamOfAnotherOrganization_thenTheNewOneIsAddedAndTheOldOneStays() {
        TeamsEntity tigres = team("Tigres", ORGANIZATION_A);
        TeamsEntity pumas = team("Pumas", ORGANIZATION_B);
        TeamMembersEntity inTigres = service.join(ana, tigres);
        inTigres.setIsCaptain(true);

        service.join(ana, pumas);

        assertThat(memberships.activeTeamsOf(ana)).containsExactlyInAnyOrder(tigres, pumas);
        assertThat(inTigres.getIsCaptain()).as("their captaincy of the other organization's team is untouched").isTrue();
        verify(accountOrganization).followTeam(EMAIL, ORGANIZATION_B);
    }

    @Test
    void join_whenThePlayerAlreadyPlaysForThatTeam_thenNothingChanges() {
        TeamsEntity tigres = team("Tigres", ORGANIZATION_A);
        TeamMembersEntity first = service.join(ana, tigres);
        first.setIsCaptain(true);

        TeamMembersEntity again = service.join(ana, tigres);

        assertThat(again).isSameAs(first);
        assertThat(again.getIsCaptain()).isTrue();
        assertThat(memberships.everyMembershipOf(ana)).hasSize(1);
    }

    // ─── an inactive team takes nobody new ──────────────────────────────────────

    @Test
    void join_whenTheTeamIsInactive_thenNobodyNewEntersAndWhatTheyHadStays() {
        TeamsEntity leones = team("Leones", ORGANIZATION_A);
        service.join(ana, leones);
        TeamsEntity tigres = team("Tigres", ORGANIZATION_A);
        tigres.setStatus(TeamStatus.INACTIVE.name());

        assertThatThrownBy(() -> service.join(ana, tigres))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TeamError.TEAM_INACTIVE.name());

        // The refusal comes before the player leaves the team they had in that organization.
        assertThat(memberships.activeTeamsOf(ana)).containsExactly(leones);
    }

    @Test
    void join_whenThePlayerWasAlreadyInATeamThatThenWentInactive_thenTheyStayInIt() {
        TeamsEntity tigres = team("Tigres", ORGANIZATION_A);
        TeamMembersEntity membership = service.join(ana, tigres);
        tigres.setStatus(TeamStatus.INACTIVE.name());

        assertThat(service.join(ana, tigres)).isSameAs(membership);
        assertThat(memberships.activeTeamsOf(ana)).containsExactly(tigres);
    }

    @Test
    void end_whenTheMembershipEnds_thenTheTeamAdministrationEndsToo() {
        TeamMembersEntity inTigres = service.join(ana, team("Tigres", ORGANIZATION_A));
        inTigres.setIsTeamAdmin(true);

        service.leave(inTigres);

        assertThat(inTigres.isAdministratorOfTheTeam()).isFalse();
    }

    @Test
    void join_whenTheTeamsOfThePlatformOwnerHaveNoOrganization_thenTheyCountAsOneOrganization() {
        TeamsEntity first = team("Del propietario 1", null);
        TeamsEntity second = team("Del propietario 2", null);
        service.join(ana, first);

        service.join(ana, second);

        assertThat(memberships.activeTeamsOf(ana)).containsExactly(second);
    }

    @Test
    void leave_whenTheyStillPlayForATeamOfAnotherOrganization_thenTheAccountFollowsThatOne() {
        TeamMembersEntity inTigres = service.join(ana, team("Tigres", ORGANIZATION_A));
        service.join(ana, team("Pumas", ORGANIZATION_B));

        service.leave(inTigres);

        assertThat(memberships.activeTeamsOf(ana)).extracting(TeamsEntity::getName).containsExactly("Pumas");
        // The last call is the one that matters: the account is left in the organization of the team that remains.
        verify(accountOrganization, org.mockito.Mockito.atLeastOnce()).followTeam(EMAIL, ORGANIZATION_B);
    }

    @Test
    void leave_whenItWasTheirOnlyTeam_thenTheAccountLosesTheOrganization() {
        TeamMembersEntity inTigres = service.join(ana, team("Tigres", ORGANIZATION_A));

        service.leave(inTigres);

        assertThat(memberships.activeTeamsOf(ana)).isEmpty();
        verify(accountOrganization).followTeam(EMAIL, null);
    }

    @Test
    void leaveEvery_whenThePlayerPlaysForSeveralTeams_thenTheyLeaveAllOfThem() {
        service.join(ana, team("Tigres", ORGANIZATION_A));
        service.join(ana, team("Pumas", ORGANIZATION_B));

        service.leaveEvery(ana);

        assertThat(memberships.activeTeamsOf(ana)).isEmpty();
        assertThat(memberships.everyMembershipOf(ana)).as("kept as history").hasSize(2);
        verify(accountOrganization).followTeam(EMAIL, null);
    }

    @Test
    void leaveEvery_whenThePlayerHasNoTeam_thenTheAccountIsStillLeftWithoutOrganization() {
        service.leaveEvery(ana);

        verify(accountOrganization).followTeam(EMAIL, null);
        verify(accountOrganization, never()).followTeam(EMAIL, ORGANIZATION_A);
    }
}
