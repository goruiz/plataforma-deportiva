package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MatchResultPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock private MatchSettingsReader settingsReader;

    private MatchResultPolicy policy;

    private void givenSettings(boolean drawsAllowed, boolean penaltiesEnabled) {
        lenient().when(settingsReader.of(any())).thenReturn(new SettingsValues(15, 5, drawsAllowed, 0, penaltiesEnabled, 48));
        policy = new MatchResultPolicy(settingsReader);
    }

    private static MatchesEntity matchWithoutResult(MatchStage stage) {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        match.setEventId(UUID.randomUUID());
        match.setStage(stage);
        return match;
    }

    /** Applies a result to the match as the update does, then runs the policy against what it was before. */
    private void record(MatchesEntity match, Integer home, Integer away, Integer homePenalties, Integer awayPenalties) {
        MatchResultPolicy.Result before = MatchResultPolicy.Result.of(match);
        match.setHomeScore(home);
        match.setAwayScore(away);
        if (homePenalties != null) match.setHomePenaltyScore(homePenalties);
        if (awayPenalties != null) match.setAwayPenaltyScore(awayPenalties);
        policy.enforce(match, before, NOW);
    }

    private static void assertRefusedWith(Runnable recording, String field, MatchError error) {
        assertThatThrownBy(recording::run).isInstanceOfSatisfying(FieldValidationException.class,
                refused -> assertThat(refused.getFieldErrors()).containsEntry(field, error.name()));
    }

    // ─── draws ───────────────────────────────────────────────────────────────────

    @Test
    void aLevelResultIsFineWhenDrawsAreAllowed() {
        givenSettings(true, false);
        MatchesEntity match = matchWithoutResult(null);

        assertThatCode(() -> record(match, 1, 1, null, null)).doesNotThrowAnyException();
    }

    @Test
    void aLevelLeagueResultIsRefusedWhenDrawsAreNotAllowed() {
        givenSettings(false, false);
        MatchesEntity match = matchWithoutResult(MatchStage.LEAGUE);

        assertRefusedWith(() -> record(match, 2, 2, null, null), "homeScore", MatchError.DRAW_NOT_ALLOWED);
    }

    @Test
    void aMatchWithoutAStageCountsAsALeagueMatchForDraws() {
        givenSettings(false, false);

        assertRefusedWith(() -> record(matchWithoutResult(null), 0, 0, null, null), "homeScore", MatchError.DRAW_NOT_ALLOWED);
    }

    @Test
    void aDecisiveResultIsFineWhenDrawsAreNotAllowed() {
        givenSettings(false, false);

        assertThatCode(() -> record(matchWithoutResult(MatchStage.GROUP), 3, 1, null, null)).doesNotThrowAnyException();
    }

    @Test
    void anEliminationMayEndLevelEvenWhenDrawsAreNotAllowed() {
        givenSettings(false, false);

        assertThatCode(() -> record(matchWithoutResult(MatchStage.KNOCKOUT), 1, 1, null, null)).doesNotThrowAnyException();
    }

    @Test
    void aLevelLeagueResultSettledByPenaltiesIsFineWhenDrawsAreNotAllowed() {
        givenSettings(false, true);

        assertThatCode(() -> record(matchWithoutResult(MatchStage.LEAGUE), 1, 1, 4, 3)).doesNotThrowAnyException();
    }

    @Test
    void anUnchangedLevelResultIsNotRefusedForARuleThatCameLater() {
        givenSettings(false, false);
        MatchesEntity match = matchWithoutResult(MatchStage.LEAGUE);
        match.setHomeScore(1);
        match.setAwayScore(1);

        assertThatCode(() -> record(match, 1, 1, null, null)).doesNotThrowAnyException();
    }

    // ─── penalties ───────────────────────────────────────────────────────────────

    @Test
    void penaltiesAreRefusedWhenTheSettingsDoNotAllowThem() {
        givenSettings(true, false);

        assertRefusedWith(() -> record(matchWithoutResult(MatchStage.KNOCKOUT), 1, 1, 4, 3),
                "homePenaltyScore", MatchError.PENALTIES_NOT_ENABLED);
    }

    @Test
    void penaltiesNeedALevelResult() {
        givenSettings(true, true);

        assertRefusedWith(() -> record(matchWithoutResult(MatchStage.KNOCKOUT), 2, 1, 4, 3),
                "homePenaltyScore", MatchError.PENALTIES_REQUIRE_LEVEL_RESULT);
    }

    @Test
    void penaltiesNeedBothScores() {
        givenSettings(true, true);

        assertRefusedWith(() -> record(matchWithoutResult(MatchStage.KNOCKOUT), 1, 1, 4, null),
                "homePenaltyScore", MatchError.PENALTIES_INCOMPLETE);
    }

    @Test
    void aShootoutHasAWinner() {
        givenSettings(true, true);

        assertRefusedWith(() -> record(matchWithoutResult(MatchStage.KNOCKOUT), 1, 1, 3, 3),
                "homePenaltyScore", MatchError.PENALTIES_MUST_DECIDE);
    }

    @Test
    void aPenaltyScoreCannotBeNegative() {
        givenSettings(true, true);

        assertRefusedWith(() -> record(matchWithoutResult(MatchStage.KNOCKOUT), 1, 1, -1, 3),
                "homePenaltyScore", MatchError.INVALID_PENALTY_SCORE);
    }

    @Test
    void aLevelResultWithAValidShootoutIsKept() {
        givenSettings(true, true);
        MatchesEntity match = matchWithoutResult(MatchStage.KNOCKOUT);

        record(match, 1, 1, 5, 4);

        assertThat(match.hasPenaltyShootout()).isTrue();
        assertThat(match.getHomePenaltyScore()).isEqualTo(5);
    }

    @Test
    void correctingAShootoutResultToADecisiveOneDropsTheShootout() {
        givenSettings(true, true);
        MatchesEntity match = matchWithoutResult(MatchStage.KNOCKOUT);
        record(match, 1, 1, 5, 4);

        record(match, 2, 1, null, null);

        assertThat(match.getHomePenaltyScore()).isNull();
        assertThat(match.getAwayPenaltyScore()).isNull();
    }

    // ─── bookkeeping ─────────────────────────────────────────────────────────────

    @Test
    void aNewResultIsStampedAndLeavesTheMatchUnvalidated() {
        givenSettings(true, false);
        MatchesEntity match = matchWithoutResult(null);
        match.setResultValidatedAt(NOW.minusDays(1));
        match.setResultValidatedBy(UUID.randomUUID());

        record(match, 2, 0, null, null);

        assertThat(match.getResultRecordedAt()).isEqualTo(NOW);
        assertThat(match.getResultValidatedAt()).isNull();
        assertThat(match.getResultValidatedBy()).isNull();
    }

    @Test
    void sendingTheSameResultAgainKeepsItsValidationAndItsMoment() {
        givenSettings(true, false);
        MatchesEntity match = matchWithoutResult(null);
        match.setHomeScore(2);
        match.setAwayScore(0);
        match.setResultRecordedAt(NOW.minusDays(1));
        match.setResultValidatedAt(NOW.minusHours(2));

        record(match, 2, 0, null, null);

        assertThat(match.getResultRecordedAt()).isEqualTo(NOW.minusDays(1));
        assertThat(match.getResultValidatedAt()).isEqualTo(NOW.minusHours(2));
    }
}
