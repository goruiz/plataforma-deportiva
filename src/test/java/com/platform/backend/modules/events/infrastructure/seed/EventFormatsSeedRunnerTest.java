package com.platform.backend.modules.events.infrastructure.seed;

import com.platform.backend.modules.events.application.iservices.IEventFormatsService;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventFormatsSeedRunnerTest {

    private static final String SEED_ENABLED_PROPERTY = "app.seed.event-formats.enabled";

    @Mock private IEventFormatRepository eventFormatRepository;
    @Mock private IEventFormatsService eventFormatsService;

    @InjectMocks
    private EventFormatsSeedRunner seedRunner;

    @Test
    void run_whenNoFormatExists_thenCreateTheWholeCatalog() {
        when(eventFormatRepository.existsByName(anyString())).thenReturn(false);

        seedRunner.run();

        verify(eventFormatsService, times(EventFormatCatalog.all().size())).create(any());
    }

    @Test
    void run_whenAFormatAlreadyExists_thenLeaveItAloneAndCreateTheRest() {
        String existingName = EventFormatCatalog.all().get(0).getName();
        when(eventFormatRepository.existsByName(anyString())).thenReturn(false);
        when(eventFormatRepository.existsByName(existingName)).thenReturn(true);

        seedRunner.run();

        verify(eventFormatsService, never()).create(argThat(format -> existingName.equals(format.getName())));
        verify(eventFormatsService, times(EventFormatCatalog.all().size() - 1)).create(any());
    }

    @Test
    void run_whenAFormatCannotBeCreated_thenSkipItAndKeepSeedingTheRest() {
        String failingName = EventFormatCatalog.all().get(0).getName();
        when(eventFormatRepository.existsByName(anyString())).thenReturn(false);
        when(eventFormatsService.create(argThat(format -> failingName.equals(format.getName()))))
                .thenThrow(new IllegalStateException("database refused it"));

        assertThatCode(() -> seedRunner.run()).doesNotThrowAnyException();

        verify(eventFormatsService, times(EventFormatCatalog.all().size())).create(any());
    }

    @Test
    void run_whenTheWholeCatalogExists_thenCreateNothing() {
        when(eventFormatRepository.existsByName(anyString())).thenReturn(true);

        seedRunner.run();

        verify(eventFormatsService, never()).create(any(CreateEventFormatRequest.class));
    }

    @Test
    void seedRunner_whenThePropertyIsNotSet_thenItIsEnabled() {
        contextRunner().run(context -> assertThat(context).hasSingleBean(EventFormatsSeedRunner.class));
    }

    @Test
    void seedRunner_whenThePropertyIsFalse_thenItIsNotCreated() {
        contextRunner().withPropertyValues(SEED_ENABLED_PROPERTY + "=false")
                .run(context -> assertThat(context).doesNotHaveBean(EventFormatsSeedRunner.class));
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withBean(IEventFormatRepository.class, () -> mock(IEventFormatRepository.class))
                .withBean(IEventFormatsService.class, () -> mock(IEventFormatsService.class))
                .withUserConfiguration(EventFormatsSeedRunner.class);
    }
}
