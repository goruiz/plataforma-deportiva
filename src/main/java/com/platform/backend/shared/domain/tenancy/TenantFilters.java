package com.platform.backend.shared.domain.tenancy;

/**
 * The one Hibernate filter that keeps each organization inside its own data, and the condition every entity applies
 * with it. The filter is declared in this package's {@code package-info} and is enabled on every session, so no query
 * has to remember to narrow by organization.
 * <p>
 * The conditions are raw SQL on purpose (Hibernate takes them that way): {@code {alias}} is the entity's table alias and
 * {@code :unrestricted} / {@code :organizationId} come from the current caller ({@link TenantScope}). A restricted
 * caller only sees the rows below; an unrestricted one (the platform owner) sees all of them.
 */
public final class TenantFilters {

    public static final String NAME = "organizationScope";
    public static final String UNRESTRICTED_PARAM = "unrestricted";
    public static final String ORGANIZATION_ID_PARAM = "organizationId";

    private static final String ANY_ROW_WHEN_UNRESTRICTED = "(:" + UNRESTRICTED_PARAM + " = true OR ";

    private static final String OWN_EVENT_IDS =
            "(SELECT scope_e.id FROM events scope_e WHERE scope_e.organization_id = :" + ORGANIZATION_ID_PARAM + ")";
    private static final String OWN_TEAM_IDS =
            "(SELECT scope_t.id FROM teams scope_t WHERE scope_t.organization_id = :" + ORGANIZATION_ID_PARAM + ")";
    private static final String OWN_USER_IDS =
            "(SELECT scope_u.id FROM users scope_u WHERE scope_u.organization_id = :" + ORGANIZATION_ID_PARAM + ")";
    private static final String OWN_MATCH_IDS =
            "(SELECT scope_m.id FROM matches scope_m WHERE scope_m.event_id IN " + OWN_EVENT_IDS + ")";

    /** Rows that belong to the caller's organization. */
    public static final String OWN_ORGANIZATION =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id = :" + ORGANIZATION_ID_PARAM + ")";

    /** Catalog rows without an organization are shared by everyone (the seeded event formats); the caller's own are added. */
    public static final String OWN_OR_SHARED =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id IS NULL OR {alias}.organization_id = :"
                    + ORGANIZATION_ID_PARAM + ")";

    // Rows without an organization column take the one of the record they hang from.

    public static final String OF_OWN_EVENT = ANY_ROW_WHEN_UNRESTRICTED + "{alias}.event_id IN " + OWN_EVENT_IDS + ")";

    public static final String OF_OWN_EVENT_BY_ID_EVENT =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_event IN " + OWN_EVENT_IDS + ")";

    public static final String OF_OWN_MATCH_BY_ID_MATCH =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_match IN " + OWN_MATCH_IDS + ")";

    public static final String OF_OWN_MATCH_BY_MATCH_ID =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.match_id IN " + OWN_MATCH_IDS + ")";

    public static final String OF_OWN_TEAM = ANY_ROW_WHEN_UNRESTRICTED + "{alias}.team_id IN " + OWN_TEAM_IDS + ")";

    /** A player is global: one with no team is visible to any organization (so it can be found and added), one in a team only to its organization. */
    public static final String PLAYER_WITHOUT_TEAM_OR_OF_OWN_TEAM =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_team IS NULL OR {alias}.id_team IN " + OWN_TEAM_IDS + ")";

    public static final String OF_OWN_USER = ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_user IN " + OWN_USER_IDS + ")";

    private TenantFilters() {}
}
