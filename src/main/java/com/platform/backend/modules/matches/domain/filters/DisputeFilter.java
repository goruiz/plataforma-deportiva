package com.platform.backend.modules.matches.domain.filters;

import com.platform.backend.modules.matches.domain.enums.DisputeStatus;

import java.util.List;

/**
 * What the list of disputes is narrowed by. Null (or empty) means "no condition of that kind".
 *
 * @param searchTerms lower-cased words; each one must appear in the reason, the name of the team that disputes, the
 *                    names of the two teams of the match or the name of its event
 * @param status      only the disputes in this state
 */
public record DisputeFilter(List<String> searchTerms, DisputeStatus status) {

    public DisputeFilter withStatus(DisputeStatus other) {
        return new DisputeFilter(searchTerms, other);
    }
}
