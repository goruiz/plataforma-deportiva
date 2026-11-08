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

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "RESCHEDULED"})
    void isAwaitingKickOff_whenTheMatchIsDueAtItsDate_thenTrue(String status) {
        assertThat(MatchStatus.isAwaitingKickOff(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"IN_PROGRESS", "FINISHED", "COMPLETED", "CANCELLED", "POSTPONED", "SUSPENDED", "ABANDONED"})
    void isAwaitingKickOff_whenTheMatchWasPutOffOrIsOverOrUnderway_thenFalse(String status) {
        assertThat(MatchStatus.isAwaitingKickOff(status)).isFalse();
        assertThat(MatchStatus.isAwaitingKickOff(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "IN_PROGRESS", "FINISHED", "COMPLETED", "RESCHEDULED"})
    void occupiesCourt_whenTheMatchIsOnItsDate_thenTrue(String status) {
        assertThat(MatchStatus.occupiesCourt(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "POSTPONED", "SUSPENDED"})
    void occupiesCourt_whenTheMatchWasCalledOffOrPutOff_thenFalse(String status) {
        assertThat(MatchStatus.occupiesCourt(status)).isFalse();
    }

    @Test
    void occupiesCourt_whenTheTextIsNotAKnownStatus_thenTheCourtIsStillCountedAsTaken() {
        assertThat(MatchStatus.occupiesCourt("ABANDONED")).isTrue();
        assertThat(MatchStatus.occupiesCourt(null)).isTrue();
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
