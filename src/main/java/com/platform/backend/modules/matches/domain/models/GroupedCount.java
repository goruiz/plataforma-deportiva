package com.platform.backend.modules.matches.domain.models;

import java.util.UUID;

/**
 * One row of a grouped count: how many records the group has. What the group is depends on the query that built it (a
 * team, or a player) and {@code label} refines it when the query also groups by a code (the type of a match event);
 * null otherwise.
 */
public record GroupedCount(UUID groupId, String label, long count) {
}
