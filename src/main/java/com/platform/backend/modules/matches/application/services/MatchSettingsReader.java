package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.settings.application.iservices.ISystemSettingsService;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** The rules that apply to a match are those of the organization its event belongs to, not of whoever asks. */
@Component
@RequiredArgsConstructor
public class MatchSettingsReader {

    private final ISystemSettingsService settingsService;
    private final IEventRepository eventRepository;

    /** A match without an event (or whose event is not visible) follows the platform's rules. */
    public SettingsValues of(MatchesEntity match) {
        UUID organizationId = match.getEventId() == null
                ? null
                : eventRepository.findActiveById(match.getEventId()).map(EventsEntity::getOrganizationId).orElse(null);
        return settingsService.effectiveFor(organizationId);
    }

    /** The rules of an event the caller asks about: unlike a match's, an event that cannot be seen is an error. */
    public SettingsValues ofEvent(UUID eventId) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
        return settingsService.effectiveFor(event.getOrganizationId());
    }
}
