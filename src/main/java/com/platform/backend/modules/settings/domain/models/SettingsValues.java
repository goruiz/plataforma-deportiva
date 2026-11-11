package com.platform.backend.modules.settings.domain.models;

/**
 * The match rules in force for an organization, as the rest of the backend reads them (the entity stays inside the
 * settings module).
 *
 * @param matchWaitMinutes   how long every match waits for its teams before it starts: it lengthens the slot of the match
 * @param maxSubstitutions   how many substitutions each team may make in a match (0 = none)
 * @param drawsAllowed       whether a match that is not an elimination may end level
 * @param extraTimeMinutes   the extra time an elimination that decides a tie may need (0 = none): it lengthens the slot
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
