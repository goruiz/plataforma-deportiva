package com.platform.backend.modules.events.domain.exception;

/**
 * Why an event format request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum EventFormatError {
    NAME_ALREADY_EXISTS,
    IN_USE,
    /** A rule the chosen structure needs was not sent (or rules were sent without any structure). */
    RULE_REQUIRED,
    /** Points must go down from win to draw to loss, and a win must be worth more than a loss. */
    POINTS_ORDER_INVALID,
    TIEBREAKERS_DUPLICATED
}
