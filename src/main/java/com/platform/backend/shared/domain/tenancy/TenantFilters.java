package com.platform.backend.shared.domain.tenancy;

/**
 * The one Hibernate filter that keeps each organization inside its own data, and the condition every entity applies
 * with it. The filter is declared in this package's {@code package-info} and is enabled on every session, so no query
 * has to remember to narrow by organization.
 * <p>
 * The conditions are raw SQL on purpose (Hibernate takes them that way): {@code {alias}} is the entity's table alias and
 * the {@code :parameters} come from the current caller ({@link TenantScope}). A restricted caller only sees the rows
 * below; an unrestricted one (the platform owner) sees all of them.
 * <p>
 * Two things widen what a restricted caller sees, both for players (mobile-app accounts):
 * <ul>
 *   <li>the events marked as public, with their teams, matches and match events ({@code :includePublicEvents});</li>
 *   <li>what is about the caller: their account and its roles, their player, the team they play for and the teams that
 *       invited them ({@code :callerEmail}). Without it a player whose token does not carry the organization of
 *       their team yet could not even read their own account.</li>
 * </ul>
 */
public final class TenantFilters {

    public static final String NAME = "organizationScope";
    public static final String UNRESTRICTED_PARAM = "unrestricted";
    public static final String ORGANIZATION_ID_PARAM = "organizationId";
    public static final String INCLUDE_PUBLIC_EVENTS_PARAM = "includePublicEvents";
    public static final String CALLER_EMAIL_PARAM = "callerEmail";

    private static final String ANY_ROW_WHEN_UNRESTRICTED = "(:" + UNRESTRICTED_PARAM + " = true OR ";

    private static final String OWN_EVENT_IDS =
            "(SELECT scope_e.id FROM events scope_e WHERE scope_e.organization_id = :" + ORGANIZATION_ID_PARAM + ")";
    private static final String OWN_TEAM_IDS =
            "(SELECT scope_t.id FROM teams scope_t WHERE scope_t.organization_id = :" + ORGANIZATION_ID_PARAM + ")";
    private static final String OWN_USER_IDS =
            "(SELECT scope_u.id FROM users scope_u WHERE scope_u.organization_id = :" + ORGANIZATION_ID_PARAM + ")";

    private static final String PUBLIC_EVENT_IDS =
            "(SELECT pub_e.id FROM events pub_e WHERE pub_e.is_public = true AND pub_e.deleted_at IS NULL)";

    /** The events the caller sees: their organization's, plus the public ones when the caller is a player. */
    private static final String VISIBLE_EVENT_IDS =
            "(SELECT vis_e.id FROM events vis_e WHERE vis_e.organization_id = :" + ORGANIZATION_ID_PARAM
                    + " OR (:" + INCLUDE_PUBLIC_EVENTS_PARAM + " = true AND vis_e.is_public = true AND vis_e.deleted_at IS NULL))";

    private static final String VISIBLE_MATCH_IDS =
            "(SELECT vis_m.id FROM matches vis_m WHERE vis_m.event_id IN " + VISIBLE_EVENT_IDS + ")";

    /** The teams enrolled in a public event, so a player can read who plays in it. */
    private static final String PUBLIC_EVENT_TEAM_IDS =
            "(SELECT pub_te.team_id FROM teams_events pub_te WHERE pub_te.deleted_at IS NULL AND pub_te.event_id IN "
                    + PUBLIC_EVENT_IDS + ")";

    private static final String CALLER_USER_IDS =
            "(SELECT self_u.id FROM users self_u WHERE self_u.email = :" + CALLER_EMAIL_PARAM + ")";

    /** The team the caller plays for. */
    private static final String CALLER_TEAM_IDS =
            "(SELECT self_p.id_team FROM players self_p WHERE self_p.email = :" + CALLER_EMAIL_PARAM
                    + " AND self_p.id_team IS NOT NULL)";

    /** The teams with a pending invitation for the caller: they are shown in the tray and joined by accepting. */
    private static final String CALLER_INVITING_TEAM_IDS =
            "(SELECT self_i.team_id FROM team_invitations self_i WHERE lower(self_i.email) = :" + CALLER_EMAIL_PARAM
                    + " AND self_i.status = 'PENDING' AND self_i.deleted_at IS NULL)";

    /** Rows that belong to the caller's organization. */
    public static final String OWN_ORGANIZATION =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id = :" + ORGANIZATION_ID_PARAM + ")";

    /** Catalog rows without an organization are shared by everyone (the seeded event formats); the caller's own are added. */
    public static final String OWN_OR_SHARED =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id IS NULL OR {alias}.organization_id = :"
                    + ORGANIZATION_ID_PARAM + ")";

    /** An event of the caller's organization, or a public one when the caller is a player. */
    public static final String OWN_OR_PUBLIC_EVENT =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id = :" + ORGANIZATION_ID_PARAM
                    + " OR (:" + INCLUDE_PUBLIC_EVENTS_PARAM + " = true AND {alias}.is_public = true))";

    /** A user is the caller's own account, or one of the caller's organization. */
    public static final String OWN_ORGANIZATION_OR_SELF =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id = :" + ORGANIZATION_ID_PARAM
                    + " OR {alias}.email = :" + CALLER_EMAIL_PARAM + ")";

    /**
     * A team of the caller's organization, one enrolled in a public event, the one the caller plays for or one that
     * invited them.
     */
    public static final String OWN_OR_PUBLIC_OR_OF_CALLER_TEAM =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.organization_id = :" + ORGANIZATION_ID_PARAM
                    + " OR (:" + INCLUDE_PUBLIC_EVENTS_PARAM + " = true AND {alias}.id IN " + PUBLIC_EVENT_TEAM_IDS + ")"
                    + " OR {alias}.id IN " + CALLER_TEAM_IDS
                    + " OR {alias}.id IN " + CALLER_INVITING_TEAM_IDS + ")";

    // Rows without an organization column take the one of the record they hang from.

    public static final String OF_OWN_EVENT = ANY_ROW_WHEN_UNRESTRICTED + "{alias}.event_id IN " + OWN_EVENT_IDS + ")";

    /** Of an event the caller sees: their organization's or a public one (matches and the teams enrolled). */
    public static final String OF_VISIBLE_EVENT = ANY_ROW_WHEN_UNRESTRICTED + "{alias}.event_id IN " + VISIBLE_EVENT_IDS + ")";

    public static final String OF_OWN_EVENT_BY_ID_EVENT =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_event IN " + OWN_EVENT_IDS + ")";

    public static final String OF_OWN_MATCH_BY_ID_MATCH =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_match IN "
                    + "(SELECT scope_m.id FROM matches scope_m WHERE scope_m.event_id IN " + OWN_EVENT_IDS + "))";

    /** Of a match of a visible event, so the timeline of a public event's match can be read. */
    public static final String OF_VISIBLE_MATCH_BY_MATCH_ID =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.match_id IN " + VISIBLE_MATCH_IDS + ")";

    /** An invitation of a team of the caller's organization, or one addressed to the caller. */
    public static final String OF_OWN_TEAM_OR_TO_CALLER =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.team_id IN " + OWN_TEAM_IDS
                    + " OR lower({alias}.email) = :" + CALLER_EMAIL_PARAM + ")";

    /**
     * A player is global: one with no team is visible to any organization (so it can be found and added), one in a
     * team only to its organization. The caller always sees their own player.
     */
    public static final String PLAYER_WITHOUT_TEAM_OR_OF_OWN_TEAM_OR_SELF =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_team IS NULL OR {alias}.id_team IN " + OWN_TEAM_IDS
                    + " OR {alias}.email = :" + CALLER_EMAIL_PARAM + ")";

    /** The roles of the caller's organization's users, and the caller's own. */
    public static final String OF_OWN_USER_OR_SELF =
            ANY_ROW_WHEN_UNRESTRICTED + "{alias}.id_user IN " + OWN_USER_IDS
                    + " OR {alias}.id_user IN " + CALLER_USER_IDS + ")";

    private TenantFilters() {}
}
