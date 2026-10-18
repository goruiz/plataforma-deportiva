package com.platform.backend.modules.matches.application.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pairs teams for a round of the Swiss system or of a double elimination: each team, best ranked first, meets the
 * best ranked team it has not played yet. When that leaves someone without an opponent it backs up and tries
 * another combination; only if no combination avoids a rematch are rematches allowed.
 */
public final class PairingSearch {

    /** Stops looking for a combination without rematches after this many attempts (it matters only for big events). */
    private static final int MAX_ATTEMPTS = 200_000;

    private PairingSearch() {}

    /** @param ranked the teams, best first; there must be an even number of them */
    public static List<Pairing> pair(List<UUID> ranked, Set<Set<UUID>> alreadyPlayed) {
        if (ranked.size() % 2 != 0) {
            throw new IllegalArgumentException("An even number of teams is needed to pair them, got " + ranked.size());
        }
        return new Search(alreadyPlayed).run(ranked)
                .or(() -> new Search(Set.of()).run(ranked))
                .orElseThrow();
    }

    private static final class Search {
        private final Set<Set<UUID>> forbiddenPairs;
        private int attempts;

        private Search(Set<Set<UUID>> forbiddenPairs) {
            this.forbiddenPairs = forbiddenPairs;
        }

        Optional<List<Pairing>> run(List<UUID> remaining) {
            if (remaining.isEmpty()) {
                return Optional.of(new ArrayList<>());
            }
            UUID first = remaining.get(0);
            for (int index = 1; index < remaining.size() && attempts++ < MAX_ATTEMPTS; index++) {
                UUID candidate = remaining.get(index);
                if (forbiddenPairs.contains(Pairing.unorderedKey(first, candidate))) {
                    continue;
                }
                Optional<List<Pairing>> rest = run(withoutFirstAnd(remaining, index));
                if (rest.isPresent()) {
                    rest.get().add(0, new Pairing(first, candidate));
                    return rest;
                }
            }
            return Optional.empty();
        }

        private static List<UUID> withoutFirstAnd(List<UUID> teams, int index) {
            List<UUID> rest = new ArrayList<>(teams);
            rest.remove(index);
            rest.remove(0);
            return rest;
        }
    }
}
