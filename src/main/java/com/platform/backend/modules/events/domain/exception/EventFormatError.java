package com.platform.backend.modules.events.domain.exception;

/**
 * Why an event format request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum EventFormatError {
    NAME_ALREADY_EXISTS,
    IN_USE
}
