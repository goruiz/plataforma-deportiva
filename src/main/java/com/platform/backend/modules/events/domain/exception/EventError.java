package com.platform.backend.modules.events.domain.exception;

/**
 * Why an event request was refused. The constant's name is the stable code clients receive as the reason, so they can
 * show it in their own language.
 */
public enum EventError {
    /** The championship already has generated matches, so its format (how it is played) can no longer change. */
    FORMAT_LOCKED_BY_CHAMPIONSHIP
}
