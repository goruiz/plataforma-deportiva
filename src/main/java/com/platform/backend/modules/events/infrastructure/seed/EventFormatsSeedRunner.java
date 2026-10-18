package com.platform.backend.modules.events.infrastructure.seed;

import com.platform.backend.modules.events.application.iservices.IEventFormatsService;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the event format catalog ({@link EventFormatCatalog}: every combination of structure and yes/no rules) so
 * admins can pick any way of playing out of the box. Runs on every startup but is idempotent: a format whose name
 * already exists (created by hand, edited, or seeded before) is left untouched, so restarts never duplicate or
 * overwrite anything. Formats are created through {@link IEventFormatsService}, so seeded rules go through the same
 * validation as the ones an admin saves.
 * <p>
 * Like MenuSeedRunner it also runs in "prd": this is reference data, not test credentials. Because a deleted
 * format is created again on the next startup, set {@code app.seed.event-formats.enabled=false} once the catalog
 * has been trimmed to what the organization uses.
 * <p>
 * A format that cannot be created is logged and skipped: this catalog is optional reference data, and a problem
 * with it must never keep the whole backend from starting.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.event-formats.enabled", havingValue = "true", matchIfMissing = true)
public class EventFormatsSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EventFormatsSeedRunner.class);

    private final IEventFormatRepository eventFormatRepository;
    private final IEventFormatsService eventFormatsService;

    @Override
    public void run(String... args) {
        List<CreateEventFormatRequest> catalog = EventFormatCatalog.all();
        int created = 0;
        int failed = 0;
        for (CreateEventFormatRequest format : catalog) {
            switch (createIfMissing(format)) {
                case CREATED -> created++;
                case FAILED -> failed++;
                case ALREADY_EXISTED -> { }
            }
        }
        log.info("Event format seed done: {} created, {} already existed, {} failed.",
                created, catalog.size() - created - failed, failed);
    }

    private SeedOutcome createIfMissing(CreateEventFormatRequest format) {
        if (eventFormatRepository.existsByName(format.getName())) {
            return SeedOutcome.ALREADY_EXISTED;
        }
        try {
            eventFormatsService.create(format);
        } catch (RuntimeException error) {
            log.error("Could not seed event format '{}': {}", format.getName(), error.getMessage());
            return SeedOutcome.FAILED;
        }
        log.info("Seeded event format '{}'", format.getName());
        return SeedOutcome.CREATED;
    }

    private enum SeedOutcome { CREATED, ALREADY_EXISTED, FAILED }
}
