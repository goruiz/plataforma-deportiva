package com.platform.backend.modules.matches.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MatchStatusTest {

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "POSTPONED", "SUSPENDED", "RESCHEDULED"})
    void isPending_whenTheMatchHasNotBeenPlayed_thenTrue(String status) {
        assertThat(MatchStatus.isPending(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"IN_PROGRESS", "FINISHED", "COMPLETED", "CANCELLED"})
    void isPending_whenTheMatchWasPlayedOrIsBeingPlayedOrCalledOff_thenFalse(String status) {
        assertThat(MatchStatus.isPending(status)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "RESCHEDULED", "IN_PROGRESS"})
    void canFinishAutomatically_whenTheMatchIsMeantToBePlayedOrBeingPlayed_thenTrue(String status) {
        assertThat(MatchStatus.canFinishAutomatically(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POSTPONED", "SUSPENDED", "CANCELLED", "FINISHED", "COMPLETED"})
    void canFinishAutomatically_whenTheMatchWasNotPlayedAsScheduledOrIsAlreadyOver_thenFalse(String status) {
        assertThat(MatchStatus.canFinishAutomatically(status)).isFalse();
    }

    @Test
    void canFinishAutomatically_whenTheTextIsNotAKnownStatus_thenFalse() {
        assertThat(MatchStatus.canFinishAutomatically("ABANDONED")).isFalse();
        assertThat(MatchStatus.canFinishAutomatically(null)).isFalse();
    }

    @Test
    void autoFinishableNames_whenAsked_thenTheStoredTextsOfThoseStatuses() {
        assertThat(MatchStatus.autoFinishableNames()).containsExactlyInAnyOrder("SCHEDULED", "RESCHEDULED", "IN_PROGRESS");
    }

    @Test
    void isPending_whenTheTextIsNotAKnownStatus_thenFalse() {
        assertThat(MatchStatus.isPending("ABANDONED")).isFalse();
        assertThat(MatchStatus.isPending("scheduled")).isFalse();
        assertThat(MatchStatus.isPending(null)).isFalse();
    }
}
