package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * What the organization's settings say about the result of a match: whether it may end level and whether a penalty
 * shoot-out may settle it. It also keeps the bookkeeping of a changed result: when it was recorded, and that a result
 * that changed is no longer validated.
 */
@Component
@RequiredArgsConstructor
public class MatchResultPolicy {

    private static final String HOME_SCORE_FIELD = "homeScore";
    private static final String HOME_PENALTY_FIELD = "homePenaltyScore";

    private final MatchSettingsReader settingsReader;

    /** The result a match had at one moment: what tells whether a change altered it. */
    public record Result(Integer homeScore, Integer awayScore, Integer homePenaltyScore, Integer awayPenaltyScore) {

        public static Result of(MatchesEntity match) {
            return new Result(match.getHomeScore(), match.getAwayScore(),
                    match.getHomePenaltyScore(), match.getAwayPenaltyScore());
        }

        boolean hasPenalties() {
            return homePenaltyScore != null || awayPenaltyScore != null;
        }
    }

    /**
     * Checks the result an update left on the match against the settings, and rejects it (before anything is saved) when
     * it breaks them. A result that did not change is left alone, so a client that sends the stored result again (as the
     * mobile app does) is not refused for rules that came later.
     */
    public void enforce(MatchesEntity match, Result before, LocalDateTime now) {
        Result after = Result.of(match);
        if (after.equals(before)) {
            return;
        }
        SettingsValues settings = settingsReader.of(match);
        checkPenalties(match, before, after, settings);
        checkDraw(match, settings);
        markAsChanged(match, now);
    }

    private void checkPenalties(MatchesEntity match, Result before, Result after, SettingsValues settings) {
        if (!after.hasPenalties()) {
            return;
        }
        boolean penaltiesChanged = !Objects.equals(after.homePenaltyScore(), before.homePenaltyScore())
                || !Objects.equals(after.awayPenaltyScore(), before.awayPenaltyScore());
        if (!isLevel(match)) {
            if (penaltiesChanged) {
                throw new FieldValidationException(HOME_PENALTY_FIELD, MatchError.PENALTIES_REQUIRE_LEVEL_RESULT.name());
            }
            // A correction that made the result decisive leaves a shoot-out that no longer exists.
            match.setHomePenaltyScore(null);
            match.setAwayPenaltyScore(null);
            return;
        }
        if (!penaltiesChanged) {
            return;
        }
        if (after.homePenaltyScore() == null || after.awayPenaltyScore() == null) {
            throw new FieldValidationException(HOME_PENALTY_FIELD, MatchError.PENALTIES_INCOMPLETE.name());
        }
        if (after.homePenaltyScore() < 0 || after.awayPenaltyScore() < 0) {
            throw new FieldValidationException(HOME_PENALTY_FIELD, MatchError.INVALID_PENALTY_SCORE.name());
        }
        if (!settings.penaltiesEnabled()) {
            throw new FieldValidationException(HOME_PENALTY_FIELD, MatchError.PENALTIES_NOT_ENABLED.name());
        }
        if (after.homePenaltyScore().equals(after.awayPenaltyScore())) {
            throw new FieldValidationException(HOME_PENALTY_FIELD, MatchError.PENALTIES_MUST_DECIDE.name());
        }
    }

    /** Only an elimination always needs a winner; elsewhere a level result needs a shoot-out when draws are off. */
    private void checkDraw(MatchesEntity match, SettingsValues settings) {
        if (settings.drawsAllowed() || !isLevel(match) || isElimination(match) || hasDecisiveShootout(match)) {
            return;
        }
        throw new FieldValidationException(HOME_SCORE_FIELD, MatchError.DRAW_NOT_ALLOWED.name());
    }

    private static boolean hasDecisiveShootout(MatchesEntity match) {
        return match.hasPenaltyShootout() && !match.getHomePenaltyScore().equals(match.getAwayPenaltyScore());
    }

    private static boolean isLevel(MatchesEntity match) {
        return match.hasResult() && match.getHomeScore().equals(match.getAwayScore());
    }

    private static boolean isElimination(MatchesEntity match) {
        MatchStage stage = match.getStage();
        return stage != null && stage.phase().isElimination();
    }

    /** A new or corrected result restarts the dispute window and has to be validated again. */
    private static void markAsChanged(MatchesEntity match, LocalDateTime now) {
        match.setResultRecordedAt(now);
        match.setResultValidatedAt(null);
        match.setResultValidatedBy(null);
    }
}
