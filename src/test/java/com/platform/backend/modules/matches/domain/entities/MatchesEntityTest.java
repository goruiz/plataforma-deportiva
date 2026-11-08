package com.platform.backend.modules.matches.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchesEntityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 12, 0);

    private final UUID home = UUID.randomUUID();
    private final UUID away = UUID.randomUUID();

    private MatchesEntity match(LocalDateTime kickOff, String status) {
        MatchesEntity match = new MatchesEntity();
        match.setHomeTeamId(home);
        match.setAwayTeamId(away);
        match.setMatchDate(kickOff);
        match.setStatus(status);
        return match;
    }

    @Test
    void hasResult_whenBothScoresAreRecorded_thenTrueWhateverTheStatus() {
        MatchesEntity match = match(NOW, "CANCELLED");
        match.setHomeScore(2);
        match.setAwayScore(2);

        assertThat(match.hasResult()).isTrue();
    }

    @Test
    void hasResult_whenOneOrBothScoresAreMissing_thenFalse() {
        MatchesEntity match = match(NOW, "FINISHED");
        assertThat(match.hasResult()).isFalse();

        match.setHomeScore(1);
        assertThat(match.hasResult()).isFalse();

        match.setHomeScore(null);
        match.setAwayScore(1);
        assertThat(match.hasResult()).isFalse();
    }

    @Test
    void hasResult_whenTheScoreIsZeroToZero_thenItIsStillAResult() {
        MatchesEntity match = match(NOW, "FINISHED");
        match.setHomeScore(0);
        match.setAwayScore(0);

        assertThat(match.hasResult()).isTrue();
    }

    @Test
    void isPlayedBy_whenTheTeamIsHomeOrAway_thenTrue() {
        MatchesEntity match = match(NOW, "SCHEDULED");

        assertThat(match.isPlayedBy(home)).isTrue();
        assertThat(match.isPlayedBy(away)).isTrue();
    }

    @Test
    void isPlayedBy_whenTheTeamIsAnotherOrNull_thenFalse() {
        MatchesEntity match = match(NOW, "SCHEDULED");

        assertThat(match.isPlayedBy(UUID.randomUUID())).isFalse();
        assertThat(match.isPlayedBy(null)).isFalse();
    }

    @Test
    void hasStarted_whenTheKickOffHasPassed_thenTrue() {
        assertThat(match(NOW.minusMinutes(1), "SCHEDULED").hasStarted(NOW)).isTrue();
        assertThat(match(NOW, "SCHEDULED").hasStarted(NOW)).isTrue();
    }

    @Test
    void hasStarted_whenTheKickOffIsAhead_thenFalse() {
        assertThat(match(NOW.plusMinutes(1), "SCHEDULED").hasStarted(NOW)).isFalse();
    }

    @Test
    void hasStarted_whenThePostponedMatchWasDueInThePast_thenItNeverStarted() {
        assertThat(match(NOW.minusDays(1), "POSTPONED").hasStarted(NOW)).isFalse();
    }
}
