package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.events.domain.exception.CategoryEligibilityError;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.domain.models.CategoryRequirements;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.player;
import static com.platform.backend.modules.players.application.services.PlayersServiceTestSupport.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/** Every way into a team goes through {@code join}, so it is where the category of the team decides who may play. */
@ExtendWith(MockitoExtension.class)
class TeamMembershipsServiceEligibilityTest {

    private static final UUID ORGANIZATION = UUID.randomUUID();

    @Mock private IPlayerAccountOrganizationService accountOrganization;
    @Mock private ICategoryRepository categoryRepository;

    private InMemoryTeamMembers memberships;
    private TeamMembershipsService service;
    private TeamsEntity juvenileWomen;
    private PlayersEntity ana;

    @BeforeEach
    void setUp() {
        memberships = new InMemoryTeamMembers();
        service = new TeamMembershipsService(memberships, accountOrganization, new PlayerEligibilityPolicy(categoryRepository));
        UUID categoryId = UUID.randomUUID();
        juvenileWomen = team("Leonas", ORGANIZATION);
        juvenileWomen.setCategoryId(categoryId);
        when(categoryRepository.findRequirementsByIds(anyCollection()))
                .thenReturn(Map.of(categoryId, new CategoryRequirements("FEMALE", 12, 15)));
        ana = player("ana@email.com");
    }

    @Test
    void join_whenThePlayerMeetsTheCategory_thenTheyPlayForTheTeam() {
        ana.setGender("FEMALE");
        ana.setBirthDate(LocalDate.now().minusYears(13));

        assertThat(service.join(ana, juvenileWomen).getTeam()).isSameAs(juvenileWomen);
    }

    @Test
    void join_whenTheGenderIsNotTheOneOfTheCategory_thenTheyDoNotJoin() {
        ana.setGender("MALE");
        ana.setBirthDate(LocalDate.now().minusYears(13));

        assertThatThrownBy(() -> service.join(ana, juvenileWomen))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CategoryEligibilityError.CATEGORY_GENDER_NOT_ALLOWED.name());
        assertThat(memberships.findAllActiveByPlayerId(ana.getId())).isEmpty();
    }

    @Test
    void join_whenThePlayerIsTooOld_thenTheyDoNotJoin() {
        ana.setGender("FEMALE");
        ana.setBirthDate(LocalDate.now().minusYears(30));

        assertThatThrownBy(() -> service.join(ana, juvenileWomen))
                .hasMessage(CategoryEligibilityError.CATEGORY_TOO_OLD.name());
    }

    @Test
    void join_whenThePlayerHasNotCompletedTheirProfile_thenTheyDoNotJoinYet() {
        assertThatThrownBy(() -> service.join(ana, juvenileWomen))
                .hasMessage(CategoryEligibilityError.CATEGORY_GENDER_REQUIRED.name());
    }
}
