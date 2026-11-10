package com.platform.backend.modules.audit.domain.enums;

/**
 * The kinds of record whose changes the audit trail lists. The constant's name is the stable code clients receive, so
 * they can show it in their own language. Adding one also needs its source in {@code AuditSource}.
 */
public enum AuditEntityType {
    EVENT,
    /** The enrollment of a team in an event. */
    TEAM_EVENT,
    TEAM,
    /** A player joining a team or leaving it (the team's roster). */
    TEAM_MEMBER,
    MATCH,
    /** A goal, an assist, a card or a foul of a match. */
    MATCH_EVENT,
    CATEGORY,
    COURT,
    SPORT_COMPLEX,
    HOLIDAY,
    EVENT_FORMAT,
    USER,
    ORGANIZATION
}
