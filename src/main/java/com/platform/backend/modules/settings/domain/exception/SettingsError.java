package com.platform.backend.modules.settings.domain.exception;

/**
 * Why a settings request was refused. The constant's name is the stable code clients receive as the reason of the
 * offending field, so they can show it in their own language.
 */
public enum SettingsError {
    /** The number is outside what {@code app.settings.*} allows. */
    OUT_OF_RANGE
}
