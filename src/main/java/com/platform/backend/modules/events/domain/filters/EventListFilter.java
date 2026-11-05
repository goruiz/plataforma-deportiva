package com.platform.backend.modules.events.domain.filters;

import java.util.List;
import java.util.UUID;

/**
 * What a list of active events is narrowed by. Null (or empty) means "no condition of that kind".
 *
 * @param searchTerms   lower-cased words; each one must appear in the name or the description of the event
 * @param status        the event is in this state (see {@code EventStatus})
 * @param eventFormatId the event is played with this format
 */
public record EventListFilter(List<String> searchTerms, String status, UUID eventFormatId) {
}
