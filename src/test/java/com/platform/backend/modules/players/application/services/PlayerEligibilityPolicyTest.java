package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.events.domain.exception.CategoryEligibilityError;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.domain.models.CategoryRequirements;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlayerEligibilityPolicyTest {

    @Mock private ICategoryRepository categoryRepository;

    private PlayerEligibilityPolicy policy;
    private UUID categoryId;
    private TeamsEntity team;
    private PlayersEntity player;

    @BeforeEach
    void setUp() {
        policy = new PlayerEligibilityPolicy(categoryRepository);
        categoryId = UUID.randomUUID();
        team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setCategoryId(categoryId);
        player = new PlayersEntity();
        player.setId(UUID.randomUUID());
    }

    private void categoryAsks(CategoryRequirements requirements) {
        lenient().when(categoryRepository.findRequirementsByIds(anyCollection())).thenReturn(Map.of(categoryId, requirements));
    }

    @Test
    void aTeamWithoutCategoryAdmitsEveryone() {
        team.setCategoryId(null);

        assertThat(policy.reasonWhyNot(player, team)).isEmpty();
        assertThatCode(() -> policy.requireEligible(player, team)).doesNotThrowAnyException();
    }

    @Test
    void aCategoryWithoutRulesAdmitsEveryoneWithoutData() {
        categoryAsks(CategoryRequirements.NONE);

        assertThatCode(() -> policy.requireEligible(player, team)).doesNotThrowAnyException();
    }

    @Test
    void joining_whenThePlayerDoesNotMeetTheCategory_thenItIsRefusedWithTheReasonAsItsMessage() {
        categoryAsks(new CategoryRequirements("FEMALE", null, null));
        player.setGender("MALE");

        assertThatThrownBy(() -> policy.requireEligible(player, team))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CategoryEligibilityError.CATEGORY_GENDER_NOT_ALLOWED.name());
    }

    @Test
    void joining_whenThePlayerHasNotGivenTheDataTheCategoryNeeds_thenItIsRefused() {
        categoryAsks(new CategoryRequirements(null, 12, 15));

        assertThatThrownBy(() -> policy.requireEligible(player, team))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CategoryEligibilityError.CATEGORY_BIRTH_DATE_REQUIRED.name());
    }

    @Test
    void joining_whenAPersonRegistersWithTheirData_thenThatDataIsWhatIsJudged() {
        categoryAsks(new CategoryRequirements("MALE", 12, 15));

        assertThatCode(() -> policy.requireEligible("MALE", LocalDate.now().minusYears(13), team)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireEligible("MALE", LocalDate.now().minusYears(20), team))
                .hasMessage(CategoryEligibilityError.CATEGORY_TOO_OLD.name());
    }

    @Test
    void inviting_whenThePlayerIsDefinitelyExcluded_thenItIsRefused() {
        categoryAsks(new CategoryRequirements("FEMALE", null, null));
        player.setGender("MALE");

        assertThatThrownBy(() -> policy.requireNotExcluded(player, team))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CategoryEligibilityError.CATEGORY_GENDER_NOT_ALLOWED.name());
    }

    @Test
    void inviting_whenThePlayerOnlyLacksData_thenTheyCanStillBeInvitedToCompleteTheirProfile() {
        categoryAsks(new CategoryRequirements("FEMALE", 12, null));

        assertThatCode(() -> policy.requireNotExcluded(player, team)).doesNotThrowAnyException();
    }

    @Test
    void theRulesAreReadOnceForAllTheTeamsOfARoster() {
        TeamsEntity withoutCategory = new TeamsEntity();
        withoutCategory.setId(UUID.randomUUID());
        categoryAsks(CategoryRequirements.NONE);

        Map<UUID, CategoryRequirements> requirements = policy.requirementsOfCategoriesOf(List.of(team, withoutCategory));

        assertThat(requirements).containsOnlyKeys(categoryId);
        assertThat(PlayerEligibilityPolicy.requirementFor(requirements, null)).isEqualTo(CategoryRequirements.NONE);
    }

    @Test
    void whenNoTeamHasACategory_thenNothingIsRead() {
        team.setCategoryId(null);

        Map<UUID, CategoryRequirements> requirements = policy.requirementsOfCategoriesOf(List.of(team));

        assertThat(requirements).isEmpty();
        assertThat(PlayerEligibilityPolicy.requirementFor(requirements, null)).isEqualTo(CategoryRequirements.NONE);
        verifyNoInteractions(categoryRepository);
    }
}
