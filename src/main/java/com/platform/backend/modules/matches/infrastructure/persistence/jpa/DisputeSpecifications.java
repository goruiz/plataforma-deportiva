package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.filters.DisputeFilter;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of active disputes as a JPA query. It is built with the criteria API (not a native query) on purpose: only
 * entity queries go through the organization filter, so a native one would show every organization's disputes to an
 * administrator of just one.
 */
public final class DisputeSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private DisputeSpecifications() {}

    public static Specification<MatchDisputesEntity> matching(DisputeFilter filter) {
        return (dispute, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(builder.isNull(dispute.get("deletedAt")));
            if (filter.status() != null) {
                conditions.add(builder.equal(dispute.get("status"), filter.status().name()));
            }
            if (filter.searchTerms() != null) {
                filter.searchTerms().forEach(term -> conditions.add(mentions(dispute, query, builder, term)));
            }
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }

    /** "ana perez" needs every word somewhere: in the reason, or in a name of the team, the match or the event. */
    private static Predicate mentions(Root<MatchDisputesEntity> dispute, AbstractQuery<?> query,
                                      CriteriaBuilder builder, String term) {
        String pattern = "%" + escapeLikeWildcards(term) + "%";
        return builder.or(
                builder.like(builder.lower(dispute.get("reason")), pattern, LIKE_ESCAPE),
                builder.exists(teamNamed(query, builder, dispute.get("teamId"), pattern)),
                builder.exists(matchMentioning(dispute, query, builder, pattern)));
    }

    private static Subquery<Integer> teamNamed(AbstractQuery<?> query, CriteriaBuilder builder,
                                               Expression<?> teamId, String pattern) {
        Subquery<Integer> teams = query.subquery(Integer.class);
        Root<TeamsEntity> team = teams.from(TeamsEntity.class);
        return teams.select(builder.literal(1)).where(
                builder.equal(team.get("id"), teamId),
                builder.like(builder.lower(team.get("name")), pattern, LIKE_ESCAPE));
    }

    private static Subquery<Integer> matchMentioning(Root<MatchDisputesEntity> dispute, AbstractQuery<?> query,
                                                     CriteriaBuilder builder, String pattern) {
        Subquery<Integer> matches = query.subquery(Integer.class);
        Root<MatchesEntity> match = matches.from(MatchesEntity.class);
        return matches.select(builder.literal(1)).where(
                builder.equal(match.get("id"), dispute.get("matchId")),
                builder.or(
                        builder.exists(teamNamed(matches, builder, match.get("homeTeamId"), pattern)),
                        builder.exists(teamNamed(matches, builder, match.get("awayTeamId"), pattern)),
                        builder.exists(eventNamed(matches, builder, match, pattern))));
    }

    private static Subquery<Integer> eventNamed(AbstractQuery<?> query, CriteriaBuilder builder,
                                                Root<MatchesEntity> match, String pattern) {
        Subquery<Integer> events = query.subquery(Integer.class);
        Root<EventsEntity> event = events.from(EventsEntity.class);
        return events.select(builder.literal(1)).where(
                builder.equal(event.get("id"), match.get("eventId")),
                builder.like(builder.lower(event.get("name")), pattern, LIKE_ESCAPE));
    }

    /** A typed % or _ is a letter to look for, not a wildcard. */
    private static String escapeLikeWildcards(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
