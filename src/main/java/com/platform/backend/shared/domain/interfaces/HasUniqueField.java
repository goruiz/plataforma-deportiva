package com.platform.backend.shared.domain.interfaces;

import java.util.UUID;

/**
 * Implemented by entities that hold unique-constrained fields (e.g. name, email).
 * When soft-deleted, unique fields are released so the same value can be reused.
 */
public interface HasUniqueField {

    void releaseUniqueFields(UUID entityId);
}
