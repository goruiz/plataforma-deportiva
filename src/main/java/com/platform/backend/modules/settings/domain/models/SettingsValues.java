package com.platform.backend.modules.settings.domain.models;

/**
 * The match rules in force for an organization, as the rest of the backend reads them (the entity stays inside the
 * settings module).
 *
 * @param matchWaitMinutes   how long a team waits for the other before the match is given up; informational
 * @param maxSubstitutions   how many substitutions each team may make; informational
 * @param drawsAllowed       whether a match that is not an elimination may end level
 * @param extraTimeMinutes   the extra time played when an elimination ends level (0 = none); informational
 * @param penaltiesEnabled   whether a level result may be settled with a penalty shoot-out
 * @param disputeWindowHours how long after a result is recorded a team may dispute it
 */
public record SettingsValues(
        int matchWaitMinutes,
        int maxSubstitutions,
        boolean drawsAllowed,
        int extraTimeMinutes,
        boolean penaltiesEnabled,
        int disputeWindowHours) {
}
