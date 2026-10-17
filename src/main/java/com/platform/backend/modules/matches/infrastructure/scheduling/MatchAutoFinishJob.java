package com.platform.backend.modules.matches.infrastructure.scheduling;

import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks as FINISHED the matches whose time is over, so nobody has to do it by hand. It also runs once at startup,
 * which catches up on whatever ended while the backend was down. Idempotent: a match already finished is not touched.
 */
@Component
@RequiredArgsConstructor
public class MatchAutoFinishJob {

    private static final Logger log = LoggerFactory.getLogger(MatchAutoFinishJob.class);

    private final IMatchesService matchesService;

    @Scheduled(fixedDelayString = "${app.matches.auto-finish-interval-ms:60000}")
    public void finishEndedMatches() {
        int finished = matchesService.finishEndedMatches();
        if (finished > 0) {
            log.info("Marked {} match(es) as FINISHED because their time is over", finished);
        }
    }
}
