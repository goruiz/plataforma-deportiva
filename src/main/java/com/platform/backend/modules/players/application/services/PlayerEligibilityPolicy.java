package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.events.domain.exception.CategoryEligibilityError;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.domain.models.CategoryRequirements;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Whether a person may play for a team: the gender and the age its category asks for. A team without a category (or with
 * a category without rules) admits everyone. This is the one place that reads a team's category rules, so a person who
 * joins, a roster line and a match event all judge the same way.
 */
@Component
@RequiredArgsConstructor
public class PlayerEligibilityPolicy {

    /**
     * The reasons that only say "the person has not given this data yet": they can fix it by completing their profile, so
     * they do not stop an invitation, only joining.
     */
    private static final Set<CategoryEligibilityError> MISSING_DATA = EnumSet.of(
            CategoryEligibilityError.CATEGORY_GENDER_REQUIRED,
            CategoryEligibilityError.CATEGORY_BIRTH_DATE_REQUIRED);

    private final ICategoryRepository categoryRepository;

    /** What the category of each team asks, by the id of the category (teams without one are not in the map). */
    public Map<UUID, CategoryRequirements> requirementsOfCategoriesOf(Collection<TeamsEntity> teams) {
        Set<UUID> categoryIds = teams.stream()
                .map(TeamsEntity::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // Nothing to read when no team has a category: most teams do not.
        return categoryIds.isEmpty() ? Map.of() : categoryRepository.findRequirementsByIds(categoryIds);
    }

    /** What the category with that id asks (nothing when the team has none, or it was deleted). */
    public static CategoryRequirements requirementFor(Map<UUID, CategoryRequirements> byCategory, UUID categoryId) {
        // Not getOrDefault: an immutable map refuses a null key, and a team without a category has none.
        return categoryId == null ? CategoryRequirements.NONE : byCategory.getOrDefault(categoryId, CategoryRequirements.NONE);
    }

    public CategoryRequirements requirementsOf(TeamsEntity team) {
        return requirementFor(requirementsOfCategoriesOf(List.of(team)), team.getCategoryId());
    }

    /** Why the person may not play for the team, or empty when they may. */
    public Optional<CategoryEligibilityError> reasonWhyNot(PlayersEntity player, TeamsEntity team) {
        return reasonWhyNot(requirementsOf(team), player.getGender(), player.getBirthDate());
    }

    /** The same judgement with the rules already read (a roster reads those of its team once, not once per player). */
    public Optional<CategoryEligibilityError> reasonWhyNot(CategoryRequirements requirements, String gender,
                                                           LocalDate birthDate) {
        return requirements.check(gender, birthDate, LocalDate.now());
    }

    /** To join: the person must meet everything the category asks, including having given the data it needs. */
    public void requireEligible(PlayersEntity player, TeamsEntity team) {
        requireEligible(player.getGender(), player.getBirthDate(), team);
    }

    /** For someone who is not a player yet (registering with an invitation): what they gave is what is judged. */
    public void requireEligible(String gender, LocalDate birthDate, TeamsEntity team) {
        reasonWhyNot(requirementsOf(team), gender, birthDate).ifPresent(PlayerEligibilityPolicy::refuse);
    }

    /**
     * To invite: only a definite no stops it (wrong gender, too young, too old). Someone who has not said their gender or
     * birth date yet can still be invited, because they can complete their profile before accepting; joining then judges
     * them strictly.
     */
    public void requireNotExcluded(PlayersEntity player, TeamsEntity team) {
        reasonWhyNot(player, team)
                .filter(reason -> !MISSING_DATA.contains(reason))
                .ifPresent(PlayerEligibilityPolicy::refuse);
    }

    /** A refusal that is no field's: a 400 whose {@code message} is the reason's code. */
    private static void refuse(CategoryEligibilityError reason) {
        throw new IllegalStateException(reason.name());
    }
}
