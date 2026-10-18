package com.platform.backend.modules.matches.domain.enums;

/** Where a phase of the championship stands. */
public enum PhaseStatus {
    /** The previous phase has not finished, so this one cannot start yet. */
    PENDING,
    /** It can start: nothing has been generated for it. */
    READY,
    IN_PROGRESS,
    FINISHED
}
