package com.platform.backend.modules.matches.application.engine;

import java.util.UUID;

/**
 * A team registered in an event, as the championship sees it.
 *
 * @param groupNumber the group the team was drawn into (null until the groups are drawn)
 * @param seed        its place when the elimination rounds started, 1 being the best (null before that)
 */
public record TeamRef(UUID id, String name, Integer groupNumber, Integer seed) {
}
