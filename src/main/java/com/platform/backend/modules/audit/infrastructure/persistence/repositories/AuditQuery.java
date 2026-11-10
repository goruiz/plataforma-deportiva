package com.platform.backend.modules.audit.infrastructure.persistence.repositories;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.shared.domain.tenancy.TenantScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The native SQL of one audit request and the values it binds. It is native, and scoped here by hand, because the trail
 * reads a dozen tables at once and the organization filter of Hibernate only applies to entity queries.
 *
 * <p>Every record contributes up to three lines to a common-table expression (created, last changed, deleted) and the
 * outer query joins the author and the organization, filters, sorts and pages. The text of the SQL is built from fixed
 * fragments only; whatever the caller typed travels as a bound parameter.
 */
final class AuditQuery {

    /** The columns of the outer select, in the order the repository reads them. */
    static final String SELECT_COLUMNS = "a.entity_type, a.entity_id, a.action, a.occurred_at, a.actor_id, "
            + actorNameSql() + " AS actor_name, a.label, a.context, a.kind, a.organization_id, o.name AS organization_name";

    private static final char LIKE_ESCAPE = '!';
    private static final String CREATED_BY = "t.created_by";
    private static final String UPDATED_BY = "t.updated_by";
    private static final String DELETED_BY = "t.deleted_by";

    private final String entriesCte;
    private final String whereClause;
    private final Map<String, Object> parameters;

    private AuditQuery(String entriesCte, String whereClause, Map<String, Object> parameters) {
        this.entriesCte = entriesCte;
        this.whereClause = whereClause;
        this.parameters = parameters;
    }

    static AuditQuery of(AuditFilter filter, TenantScope scope) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> conditions = new ArrayList<>();
        addScopeCondition(scope, conditions, parameters);
        addDateConditions(filter, conditions, parameters);
        addSearchConditions(filter.searchTerms(), conditions, parameters);
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new AuditQuery(entriesCte(filter), where, parameters);
    }

    /**
     * The page's lines, most recent first; ties break on the record so a page never repeats or skips a line, and within
     * one record on what happened last (a record created and deleted in the same second lists its deletion first).
     */
    String selectSql() {
        return entriesCte + " SELECT " + SELECT_COLUMNS + joinsAndFilters()
                + " ORDER BY a.occurred_at DESC, a.entity_type, a.entity_id, " + latestActionFirstSql();
    }

    /** Ranks the actions so that the one that comes last in a record's life sorts first (DELETED, UPDATED, CREATED). */
    private static String latestActionFirstSql() {
        AuditAction[] actions = AuditAction.values();
        StringBuilder sql = new StringBuilder("CASE a.action");
        for (int index = 0; index < actions.length; index++) {
            sql.append(" WHEN '").append(actions[index].name()).append("' THEN ").append(actions.length - index);
        }
        return sql.append(" END").toString();
    }

    String countSql() {
        return entriesCte + " SELECT COUNT(*)" + joinsAndFilters();
    }

    Map<String, Object> parameters() {
        return parameters;
    }

    private String joinsAndFilters() {
        return " FROM audit_entries a LEFT JOIN users u ON u.id = a.actor_id "
                + "LEFT JOIN organizations o ON o.id = a.organization_id" + whereClause;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The union of what every table says
    // ---------------------------------------------------------------------------------------------------------------

    /** Only the branches the filter can match: a search for one kind of record never reads the other tables. */
    private static String entriesCte(AuditFilter filter) {
        List<String> branches = new ArrayList<>();
        for (AuditSource source : AuditSource.values()) {
            if (filter.entityType() != null && filter.entityType() != source.entityType()) {
                continue;
            }
            for (AuditAction action : AuditAction.values()) {
                if (filter.action() == null || filter.action() == action) {
                    branches.add(branch(source, action));
                }
            }
        }
        return "WITH audit_entries AS (" + String.join(" UNION ALL ", branches) + ")";
    }

    private static String branch(AuditSource source, AuditAction action) {
        String moment = momentColumn(action);
        String author = authorColumn(action);
        return "SELECT '" + source.entityType().name() + "' AS entity_type, t.id AS entity_id, '" + action.name()
                + "' AS action, " + moment + " AS occurred_at, " + author + " AS actor_id, "
                + source.labelSql() + " AS label, " + source.contextSql() + " AS context, "
                + source.kindSql() + " AS kind, " + source.organizationSql() + " AS organization_id "
                + "FROM " + source.table() + " t " + source.joins()
                + " WHERE " + moment + " IS NOT NULL AND " + author + " IS NOT NULL" + extraCondition(action);
    }

    private static String momentColumn(AuditAction action) {
        return switch (action) {
            case CREATED -> "t.created_at";
            case UPDATED -> "t.updated_at";
            case DELETED -> "t.deleted_at";
        };
    }

    private static String authorColumn(AuditAction action) {
        return switch (action) {
            case CREATED -> CREATED_BY;
            case UPDATED -> UPDATED_BY;
            case DELETED -> DELETED_BY;
        };
    }

    /**
     * Saving a record also stamps its change time (and deleting it too), so "changed" is a record that was touched after
     * it was created and is still alive; the change that deleted it is the deletion.
     */
    private static String extraCondition(AuditAction action) {
        return action == AuditAction.UPDATED ? " AND t.deleted_at IS NULL AND t.updated_at > t.created_at" : "";
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Conditions of the outer query
    // ---------------------------------------------------------------------------------------------------------------

    private static void addScopeCondition(TenantScope scope, List<String> conditions, Map<String, Object> parameters) {
        if (scope.restricted()) {
            conditions.add("a.organization_id = :organizationId");
            parameters.put("organizationId", scope.organizationIdForFilter());
        }
    }

    private static void addDateConditions(AuditFilter filter, List<String> conditions, Map<String, Object> parameters) {
        if (filter.from() != null) {
            conditions.add("a.occurred_at >= :fromMoment");
            parameters.put("fromMoment", filter.from());
        }
        if (filter.until() != null) {
            conditions.add("a.occurred_at < :untilMoment");
            parameters.put("untilMoment", filter.until());
        }
    }

    /** Every word must appear in the line's text or in the name of who made the change. */
    private static void addSearchConditions(List<String> terms, List<String> conditions, Map<String, Object> parameters) {
        if (terms == null) {
            return;
        }
        for (int index = 0; index < terms.size(); index++) {
            String name = "term" + index;
            conditions.add("LOWER(CONCAT_WS(' ', a.label, a.context, a.kind, " + actorNameSql() + ")) LIKE :" + name
                    + " ESCAPE '" + LIKE_ESCAPE + "'");
            parameters.put(name, "%" + escapeLikeWildcards(terms.get(index)) + "%");
        }
    }

    /** A typed {@code %} or {@code _} is text to find, not a wildcard. */
    private static String escapeLikeWildcards(String term) {
        return term.replace(String.valueOf(LIKE_ESCAPE), LIKE_ESCAPE + String.valueOf(LIKE_ESCAPE))
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }

    /** The author's name as the outer query sees it (the joined {@code users} row is aliased {@code u}). */
    private static String actorNameSql() {
        return AuditSource.personName("u");
    }
}
