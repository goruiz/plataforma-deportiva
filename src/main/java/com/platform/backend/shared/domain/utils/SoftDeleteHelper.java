package com.platform.backend.shared.domain.utils;

import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;

import java.time.LocalDateTime;

public final class SoftDeleteHelper {

    private SoftDeleteHelper() {}

    /**
     * Marks an entity as soft-deleted. If it implements {@link HasUniqueField},
     * its unique fields are released first so the same values can be reused.
     */
    public static void prepareForDeletion(BaseEntity entity) {
        if (entity instanceof HasUniqueField uniqueFieldEntity) {
            uniqueFieldEntity.releaseUniqueFields(entity.getId());
        }
        entity.setDeletedAt(LocalDateTime.now());
    }
}
