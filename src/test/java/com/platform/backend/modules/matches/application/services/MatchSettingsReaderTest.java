package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.presentation.responses.MatchRulesResponse.MatchRulesResponse;
import com.platform.backend.modules.settings.application.iservices.ISystemSettingsService;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** The rules of an event are those of its organization, whoever asks; also what the rules endpoint answers. */
@ExtendWith(MockitoExtension.class)
class MatchSettingsReaderTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Mock private ISystemSettingsService settingsService;
    @Mock private IEventRepository eventRepository;

    private MatchSettingsReader reader() {
        return new MatchSettingsReader(settingsService, eventRepository);
    }

    private static EventsEntity eventOf(UUID organizationId) {
        EventsEntity event = new EventsEntity();
        event.setOrganizationId(organizationId);
        return event;
    }

    private static SettingsValues rules(boolean drawsAllowed, boolean penaltiesEnabled) {
        return new SettingsValues(15, 5, drawsAllowed, 20, penaltiesEnabled, 72);
    }

    @Test
    void theRulesOfAnEventAreThoseOfItsOrganization() {
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.of(eventOf(ORGANIZATION_ID)));
        when(settingsService.effectiveFor(ORGANIZATION_ID)).thenReturn(rules(false, true));

        SettingsValues values = reader().ofEvent(EVENT_ID);

        assertThat(values.drawsAllowed()).isFalse();
        assertThat(values.penaltiesEnabled()).isTrue();
    }

    @Test
    void anEventOfThePlatformOwnerFollowsThePlatformRules() {
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.of(eventOf(null)));
        when(settingsService.effectiveFor(null)).thenReturn(rules(true, false));

        assertThat(reader().ofEvent(EVENT_ID).penaltiesEnabled()).isFalse();
    }

    @Test
    void anEventThatDoesNotExistOrCannotBeSeenIsNotFound() {
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reader().ofEvent(EVENT_ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void theRulesEndpointAnswersWhatTheMatchesOfTheEventFollow() {
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.of(eventOf(ORGANIZATION_ID)));
        when(settingsService.effectiveFor(ORGANIZATION_ID)).thenReturn(rules(false, true));

        MatchRulesResponse response = new MatchRulesService(reader()).getForEvent(EVENT_ID);

        assertThat(response.isDrawsAllowed()).isFalse();
        assertThat(response.isPenaltiesEnabled()).isTrue();
        assertThat(response.getExtraTimeMinutes()).isEqualTo(20);
        assertThat(response.getDisputeWindowHours()).isEqualTo(72);
    }
}
