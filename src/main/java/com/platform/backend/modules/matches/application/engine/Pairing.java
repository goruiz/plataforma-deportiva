package com.platform.backend.modules.matches.application.engine;

import java.util.Set;
import java.util.UUID;

/** Two teams that will meet: the first one plays at home. */
public record Pairing(UUID homeTeamId, UUID awayTeamId) {

    /** The same pairing with the venues swapped (the return leg). */
    public Pairing reversed() {
        return new Pairing(awayTeamId, homeTeamId);
    }

    /** The same key for "A vs B" and "B vs A", to remember which teams already met. */
    public Set<UUID> unorderedKey() {
        return unorderedKey(homeTeamId, awayTeamId);
    }

    public static Set<UUID> unorderedKey(UUID first, UUID second) {
        return Set.of(first, second);
    }
}
