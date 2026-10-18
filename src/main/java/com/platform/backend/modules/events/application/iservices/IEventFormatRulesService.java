package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;

public interface IEventFormatRulesService {

    /**
     * Checks the format's rules against its structure, once a create or update has been merged into it: every rule
     * the structure uses must be there and consistent, and the rules it does not use are cleared.
     * A format with no structure (created before rules existed) passes as long as it has no rules either.
     *
     * @throws com.platform.backend.shared.infraestructure.exceptions.FieldValidationException with every rejected
     *         field and its {@code EventFormatError} code
     */
    void validateAndNormalize(EventFormatsEntity format);
}
