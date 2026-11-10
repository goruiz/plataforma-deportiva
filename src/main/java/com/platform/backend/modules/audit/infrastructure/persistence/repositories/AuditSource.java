package com.platform.backend.modules.audit.infrastructure.persistence.repositories;

import com.platform.backend.modules.audit.domain.enums.AuditEntityType;

/**
 * Where the changes of one kind of record are read from. Every table keeps who created, last changed and deleted each
 * of its rows (see {@code BaseEntity}), so the trail is read from them instead of a log of its own.
 *
 * <p>The fragments are SQL over the row's table, always aliased {@code t}, and are fixed text, never user input. They
 * build the columns of a trail line: the record's name, where it belongs, a code that refines its type and the
 * organization that owns it (a record that has no organization column takes the one of its event).
 */
enum AuditSource {

    EVENT(AuditEntityType.EVENT, "events", "",
            "t.name", noText(), noText(), "t.organization_id"),

    TEAM_EVENT(AuditEntityType.TEAM_EVENT, "teams_events",
            "LEFT JOIN teams team ON team.id = t.team_id LEFT JOIN events ev ON ev.id = t.event_id",
            "team.name", "ev.name", noText(), "ev.organization_id"),

    TEAM(AuditEntityType.TEAM, "teams", "",
            "t.name", noText(), noText(), "t.organization_id"),

    TEAM_MEMBER(AuditEntityType.TEAM_MEMBER, "team_members",
            "LEFT JOIN players pl ON pl.id = t.player_id LEFT JOIN teams team ON team.id = t.team_id",
            personName("pl"), "team.name", noText(), "t.organization_id"),

    MATCH(AuditEntityType.MATCH, "matches",
            "LEFT JOIN teams home ON home.id = t.home_team_id LEFT JOIN teams away ON away.id = t.away_team_id "
                    + "LEFT JOIN events ev ON ev.id = t.event_id",
            versus("home", "away"), "ev.name", noText(), "ev.organization_id"),

    MATCH_EVENT(AuditEntityType.MATCH_EVENT, "match_events",
            "LEFT JOIN players pl ON pl.id = t.player_id LEFT JOIN matches m ON m.id = t.match_id "
                    + "LEFT JOIN teams home ON home.id = m.home_team_id LEFT JOIN teams away ON away.id = m.away_team_id "
                    + "LEFT JOIN events ev ON ev.id = m.event_id",
            personName("pl"), versus("home", "away"), "t.event_type", "ev.organization_id"),

    CATEGORY(AuditEntityType.CATEGORY, "categories", "",
            "t.name", noText(), noText(), "t.organization_id"),

    COURT(AuditEntityType.COURT, "courts", "",
            "t.name", noText(), noText(), "t.organization_id"),

    SPORT_COMPLEX(AuditEntityType.SPORT_COMPLEX, "sport_complexes", "",
            "t.name", noText(), noText(), "t.organization_id"),

    HOLIDAY(AuditEntityType.HOLIDAY, "holidays", "",
            "t.name", noText(), noText(), "t.organization_id"),

    EVENT_FORMAT(AuditEntityType.EVENT_FORMAT, "events_formats", "",
            "t.name", noText(), noText(), "t.organization_id"),

    USER(AuditEntityType.USER, "users", "",
            personName("t"), "t.email", noText(), "t.organization_id"),

    ORGANIZATION(AuditEntityType.ORGANIZATION, "organizations", "",
            "t.name", noText(), noText(), "t.id");

    /** Between the two teams of a match. Plain punctuation, so it reads the same in every language. */
    private static final String TEAMS_SEPARATOR = " – ";

    private final AuditEntityType entityType;
    private final String table;
    private final String joins;
    private final String labelSql;
    private final String contextSql;
    private final String kindSql;
    private final String organizationSql;

    AuditSource(AuditEntityType entityType, String table, String joins, String labelSql, String contextSql,
                String kindSql, String organizationSql) {
        this.entityType = entityType;
        this.table = table;
        this.joins = joins;
        this.labelSql = labelSql;
        this.contextSql = contextSql;
        this.kindSql = kindSql;
        this.organizationSql = organizationSql;
    }

    AuditEntityType entityType() {
        return entityType;
    }

    String table() {
        return table;
    }

    String joins() {
        return joins;
    }

    String labelSql() {
        return labelSql;
    }

    String contextSql() {
        return contextSql;
    }

    String kindSql() {
        return kindSql;
    }

    String organizationSql() {
        return organizationSql;
    }

    /** A column with nothing to say, typed so the branches of the union agree on it. */
    static String noText() {
        return "CAST(NULL AS text)";
    }

    /** The full name of the person a row of {@code alias} describes, or their email when they have no name. */
    static String personName(String alias) {
        return "COALESCE(NULLIF(TRIM(CONCAT_WS(' ', " + alias + ".first_name, " + alias + ".last_name)), ''), "
                + alias + ".email)";
    }

    /** The names of two teams side by side. */
    private static String versus(String homeAlias, String awayAlias) {
        return "CONCAT_WS('" + TEAMS_SEPARATOR + "', " + homeAlias + ".name, " + awayAlias + ".name)";
    }
}
