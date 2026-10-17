package com.platform.backend.modules.matches.infrastructure.scheduling;

import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchAutoFinishJobTest {

    @Mock private IMatchesService matchesService;

    @InjectMocks
    private MatchAutoFinishJob job;

    @Test
    void finishEndedMatches_whenItRuns_thenItAsksTheServiceToFinishEverythingThatEnded() {
        when(matchesService.finishEndedMatches()).thenReturn(3);

        job.finishEndedMatches();

        verify(matchesService).finishEndedMatches();
    }

    @Test
    void finishEndedMatches_isScheduledWithAnIntervalThatComesFromConfiguration() throws NoSuchMethodException {
        Scheduled scheduled = MatchAutoFinishJob.class.getMethod("finishEndedMatches").getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).contains("app.matches.auto-finish-interval-ms");
    }
}
