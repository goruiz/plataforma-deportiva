package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.enums.EventFormatRule;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.exception.EventFormatError;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventFormatRulesServiceTest {

    private final EventFormatRulesService rulesService = new EventFormatRulesService();

    // ─── Formats created before rules existed ───────────────────────────────────

    @Test
    void validate_whenThereIsNoStructureAndNoRules_thenPass() {
        assertThatCode(() -> rulesService.validateAndNormalize(new EventFormatsEntity())).doesNotThrowAnyException();
    }

    @Test
    void validate_whenRulesAreSentWithoutAnyStructure_thenAskForTheStructure() {
        EventFormatsEntity format = new EventFormatsEntity();
        format.setPointsForWin(3);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("structure", EventFormatError.RULE_REQUIRED)));
    }

    // ─── Round robin ────────────────────────────────────────────────────────────

    @Test
    void validate_whenRoundRobinHasAllItsRules_thenPass() {
        assertThatCode(() -> rulesService.validateAndNormalize(aRoundRobin())).doesNotThrowAnyException();
    }

    @Test
    void validate_whenRoundRobinLacksRules_thenReportEveryMissingOne() {
        EventFormatsEntity format = new EventFormatsEntity();
        format.setStructure(EventFormatStructure.ROUND_ROBIN);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(
                                entry("roundTrip", EventFormatError.RULE_REQUIRED),
                                entry("pointsForWin", EventFormatError.RULE_REQUIRED),
                                entry("pointsForDraw", EventFormatError.RULE_REQUIRED),
                                entry("pointsForLoss", EventFormatError.RULE_REQUIRED),
                                entry("tiebreakers", EventFormatError.RULE_REQUIRED)));
    }

    @Test
    void validate_whenRoundRobinHasNoTiebreakers_thenPass() {
        EventFormatsEntity format = aRoundRobin();
        format.setTiebreakers("");

        assertThatCode(() -> rulesService.validateAndNormalize(format)).doesNotThrowAnyException();
    }

    @Test
    void validate_whenRoundRobinCarriesRulesItDoesNotUse_thenClearThem() {
        EventFormatsEntity format = aRoundRobin();
        format.setGroupCount(4);
        format.setTeamsAdvancingPerGroup(2);
        format.setKnockoutRoundTrip(true);
        format.setThirdPlaceMatch(true);

        rulesService.validateAndNormalize(format);

        assertThat(format.getGroupCount()).isNull();
        assertThat(format.getTeamsAdvancingPerGroup()).isNull();
        assertThat(format.getKnockoutRoundTrip()).isNull();
        assertThat(format.getThirdPlaceMatch()).isNull();
        assertThat(format.getPointsForWin()).isEqualTo(3);
    }

    // ─── Points ─────────────────────────────────────────────────────────────────

    @Test
    void validate_whenPointsGoUpFromWinToLoss_thenRejectPointsForWin() {
        EventFormatsEntity format = aRoundRobin();
        format.setPointsForWin(1);
        format.setPointsForDraw(3);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("pointsForWin", EventFormatError.POINTS_ORDER_INVALID)));
    }

    @Test
    void validate_whenAWinIsWorthTheSameAsALoss_thenRejectPointsForWin() {
        EventFormatsEntity format = aRoundRobin();
        format.setPointsForWin(1);
        format.setPointsForDraw(1);
        format.setPointsForLoss(1);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class);
    }

    @Test
    void validate_whenAWinAndADrawAreWorthTheSame_thenPass() {
        EventFormatsEntity format = aRoundRobin();
        format.setPointsForWin(2);
        format.setPointsForDraw(2);
        format.setPointsForLoss(0);

        assertThatCode(() -> rulesService.validateAndNormalize(format)).doesNotThrowAnyException();
    }

    // ─── Tie-breakers ───────────────────────────────────────────────────────────

    @Test
    void validate_whenATiebreakerIsRepeated_thenRejectTiebreakers() {
        EventFormatsEntity format = aRoundRobin();
        format.setTiebreakers("GOALS_FOR,GOAL_DIFFERENCE,GOALS_FOR");

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("tiebreakers", EventFormatError.TIEBREAKERS_DUPLICATED)));
    }

    // ─── Knockout ───────────────────────────────────────────────────────────────

    @Test
    void validate_whenKnockoutHasItsRules_thenPassAndClearThePointsTable() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.KNOCKOUT);
        format.setKnockoutRoundTrip(false);
        format.setThirdPlaceMatch(true);

        rulesService.validateAndNormalize(format);

        assertThat(format.getRoundTrip()).isNull();
        assertThat(format.getPointsForWin()).isNull();
        assertThat(format.getPointsForDraw()).isNull();
        assertThat(format.getPointsForLoss()).isNull();
        assertThat(format.getTiebreakers()).isNull();
        assertThat(format.getThirdPlaceMatch()).isTrue();
    }

    @Test
    void validate_whenKnockoutLacksItsRules_thenReportThem() {
        EventFormatsEntity format = new EventFormatsEntity();
        format.setStructure(EventFormatStructure.KNOCKOUT);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(
                                entry("knockoutRoundTrip", EventFormatError.RULE_REQUIRED),
                                entry("thirdPlaceMatch", EventFormatError.RULE_REQUIRED)));
    }

    // ─── Groups + knockout ──────────────────────────────────────────────────────

    @Test
    void validate_whenGroupsKnockoutLacksTheGroupAndKnockoutRules_thenReportThem() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.GROUPS_KNOCKOUT);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(
                                entry("groupCount", EventFormatError.RULE_REQUIRED),
                                entry("teamsAdvancingPerGroup", EventFormatError.RULE_REQUIRED),
                                entry("knockoutRoundTrip", EventFormatError.RULE_REQUIRED),
                                entry("thirdPlaceMatch", EventFormatError.RULE_REQUIRED)));
    }

    @Test
    void validate_whenGroupsKnockoutHasEveryRule_thenKeepAllOfThem() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.GROUPS_KNOCKOUT);
        format.setGroupCount(4);
        format.setTeamsAdvancingPerGroup(2);
        format.setKnockoutRoundTrip(false);
        format.setThirdPlaceMatch(false);

        rulesService.validateAndNormalize(format);

        assertThat(format.getRoundTrip()).isFalse();
        assertThat(format.getGroupCount()).isEqualTo(4);
        assertThat(format.getTeamsAdvancingPerGroup()).isEqualTo(2);
        assertThat(format.getKnockoutRoundTrip()).isFalse();
    }

    // ─── Every structure ─────────────────────────────────────────────────────────

    @Test
    void validate_whenAnyStructureHasNoRules_thenReportExactlyTheRulesItUses() {
        for (EventFormatStructure structure : EventFormatStructure.values()) {
            EventFormatsEntity format = new EventFormatsEntity();
            format.setStructure(structure);
            Set<String> expectedFields = structure.rules().stream().map(EventFormatRule::fieldName).collect(Collectors.toSet());

            assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                    .as(structure.name())
                    .isInstanceOf(FieldValidationException.class)
                    .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors().keySet())
                            .containsExactlyInAnyOrderElementsOf(expectedFields));
        }
    }

    // ─── Swiss ──────────────────────────────────────────────────────────────────

    @Test
    void validate_whenSwissHasItsRules_thenPassAndDropTheLegsOfARoundRobin() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.SWISS);
        format.setSwissRounds(5);

        rulesService.validateAndNormalize(format);

        assertThat(format.getSwissRounds()).isEqualTo(5);
        assertThat(format.getPointsForWin()).isEqualTo(3);
        assertThat(format.getRoundTrip()).isNull();
    }

    @Test
    void validate_whenSwissLacksItsRounds_thenReportThem() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.SWISS);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("swissRounds", EventFormatError.RULE_REQUIRED)));
    }

    @Test
    void validate_whenSwissHasOutOfOrderPoints_thenRejectPointsForWin() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.SWISS);
        format.setSwissRounds(5);
        format.setPointsForWin(0);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("pointsForWin", EventFormatError.POINTS_ORDER_INVALID)));
    }

    // ─── Double elimination ─────────────────────────────────────────────────────

    @Test
    void validate_whenDoubleEliminationHasItsRules_thenPassAndDropTheThirdPlaceMatch() {
        EventFormatsEntity format = new EventFormatsEntity();
        format.setStructure(EventFormatStructure.DOUBLE_ELIMINATION);
        format.setKnockoutRoundTrip(false);
        format.setGrandFinalReset(true);
        format.setThirdPlaceMatch(true);

        rulesService.validateAndNormalize(format);

        assertThat(format.getGrandFinalReset()).isTrue();
        assertThat(format.getKnockoutRoundTrip()).isFalse();
        assertThat(format.getThirdPlaceMatch()).isNull();
    }

    @Test
    void validate_whenDoubleEliminationLacksTheGrandFinalRule_thenReportIt() {
        EventFormatsEntity format = new EventFormatsEntity();
        format.setStructure(EventFormatStructure.DOUBLE_ELIMINATION);
        format.setKnockoutRoundTrip(false);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("grandFinalReset", EventFormatError.RULE_REQUIRED)));
    }

    // ─── League / Swiss + playoffs ──────────────────────────────────────────────

    @Test
    void validate_whenLeagueKnockoutHasItsRules_thenPass() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.LEAGUE_KNOCKOUT);
        format.setPlayoffTeams(4);
        format.setKnockoutRoundTrip(false);
        format.setThirdPlaceMatch(false);

        assertThatCode(() -> rulesService.validateAndNormalize(format)).doesNotThrowAnyException();
        assertThat(format.getPlayoffTeams()).isEqualTo(4);
        assertThat(format.getRoundTrip()).isFalse();
    }

    @Test
    void validate_whenLeagueKnockoutLacksTheTeamsThatQualify_thenReportIt() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.LEAGUE_KNOCKOUT);
        format.setKnockoutRoundTrip(false);
        format.setThirdPlaceMatch(false);

        assertThatThrownBy(() -> rulesService.validateAndNormalize(format))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFieldErrors())
                        .containsOnly(entry("playoffTeams", EventFormatError.RULE_REQUIRED)));
    }

    @Test
    void validate_whenSwissKnockoutHasItsRules_thenPassAndDropTheLegsOfARoundRobin() {
        EventFormatsEntity format = aRoundRobin();
        format.setStructure(EventFormatStructure.SWISS_KNOCKOUT);
        format.setSwissRounds(5);
        format.setPlayoffTeams(8);
        format.setKnockoutRoundTrip(true);
        format.setThirdPlaceMatch(false);

        rulesService.validateAndNormalize(format);

        assertThat(format.getSwissRounds()).isEqualTo(5);
        assertThat(format.getPlayoffTeams()).isEqualTo(8);
        assertThat(format.getRoundTrip()).isNull();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private EventFormatsEntity aRoundRobin() {
        EventFormatsEntity format = new EventFormatsEntity();
        format.setStructure(EventFormatStructure.ROUND_ROBIN);
        format.setRoundTrip(false);
        format.setPointsForWin(3);
        format.setPointsForDraw(1);
        format.setPointsForLoss(0);
        format.setTiebreakers("GOAL_DIFFERENCE,GOALS_FOR");
        return format;
    }

    private java.util.Map.Entry<String, String> entry(String fieldName, EventFormatError error) {
        return java.util.Map.entry(fieldName, error.name());
    }
}
