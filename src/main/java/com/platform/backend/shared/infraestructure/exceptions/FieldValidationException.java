package com.platform.backend.shared.infraestructure.exceptions;

import java.util.Map;

/**
 * A business rule rejected one request field. GlobalException answers it exactly like a
 * Bean Validation failure (400 with a field → reason map in `data`), so clients show it
 * under the offending field and never confuse it with an authentication failure (401).
 */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(String fieldName, String reason) {
        super(fieldName + ": " + reason);
        this.fieldErrors = Map.of(fieldName, reason);
    }

    /** For a rule that rejects several fields at once, so the client can mark all of them in one answer. */
    public FieldValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors.toString());
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
