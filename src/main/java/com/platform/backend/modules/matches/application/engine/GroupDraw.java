package com.platform.backend.modules.matches.application.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Deals the teams into groups after a draw: the groups end up with the same number of teams, or one more or less. */
public final class GroupDraw {

    private GroupDraw() {}

    /**
     * @param shuffler decides the order of the draw; a test passes an identity function to get a known result
     * @return the group (1 to {@code groupCount}) of each team
     */
    public static Map<UUID, Integer> assign(List<UUID> teams, int groupCount, UnaryOperator<List<UUID>> shuffler) {
        List<UUID> drawn = shuffler.apply(teams);
        Map<UUID, Integer> groupOfTeam = new HashMap<>();
        for (int index = 0; index < drawn.size(); index++) {
            groupOfTeam.put(drawn.get(index), index % groupCount + 1);
        }
        return groupOfTeam;
    }
}
