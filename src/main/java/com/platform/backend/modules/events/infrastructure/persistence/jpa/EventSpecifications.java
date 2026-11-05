package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.enums.EventStatus;
import com.platform.backend.modules.events.domain.filters.EventListFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The list of active events as a JPA query. It is built with the criteria API (not a native query) on purpose: only
 * entity queries go through the organization filter, which is what shows a player their organization's events plus the
 * public ones and nothing else.
 */
public final class EventSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /** The states in the order they are listed: what is happening, what is coming, then what is over. */
    private static final List<EventStatus> RELEVANCE_ORDER =
            List.of(EventStatus.ACTIVE, EventStatus.UPCOMING, EventStatus.FINISHED, EventStatus.CANCELLED);

    private EventSpecifications() {}

    public static Specification<EventsEntity> matching(EventListFilter filter) {
        return (event, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(builder.isNull(event.get("deletedAt")));
            if (filter.status() != null) {
                conditions.add(builder.equal(event.get("status"), filter.status()));
            }
            if (filter.eventFormatId() != null) {
                conditions.add(builder.equal(event.get("eventFormat").get("id"), filter.eventFormatId()));
            }
            conditions.addAll(containsEveryTerm(event, builder, filter.searchTerms()));
            if (isTheListQuery(query)) {
                fetchWhatEachRowShows(event);
                query.orderBy(relevanceOrder(event, builder));
            }
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }

    /** Spring Data runs the specification again for the count, which has no rows to order or fetch. */
    private static boolean isTheListQuery(CriteriaQuery<?> query) {
        return EventsEntity.class.equals(query.getResultType());
    }

    /** The response names the type and the format, so they come with the page instead of one query per event. */
    private static void fetchWhatEachRowShows(Root<EventsEntity> event) {
        event.fetch("eventType", JoinType.LEFT);
        event.fetch("eventFormat", JoinType.LEFT);
    }

    /** One condition per word, all of them required: "copa quito" finds the Copa de Quito, not every Copa or Quito. */
    private static List<Predicate> containsEveryTerm(Root<EventsEntity> event, CriteriaBuilder builder, List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }
        Expression<String> name = builder.lower(event.get("name"));
        Expression<String> description = builder.lower(builder.coalesce(event.<String>get("description"), ""));
        return terms.stream()
                .map(term -> "%" + escapeLikeWildcards(term) + "%")
                .map(pattern -> builder.or(
                        builder.like(name, pattern, LIKE_ESCAPE),
                        builder.like(description, pattern, LIKE_ESCAPE)))
                .toList();
    }

    /**
     * What is happening first, then what is coming (the soonest first), then what is over (the latest first). The id
     * breaks ties so that a page never repeats or skips a row.
     */
    private static List<Order> relevanceOrder(Root<EventsEntity> event, CriteriaBuilder builder) {
        Expression<Integer> rank = statusRank(event, builder);
        Expression<LocalDate> startOfWhatIsNotOver = builder.<LocalDate>selectCase()
                .when(builder.lessThanOrEqualTo(rank, RELEVANCE_ORDER.indexOf(EventStatus.UPCOMING)), event.<LocalDate>get("startDate"))
                .otherwise(builder.nullLiteral(LocalDate.class));
        return List.of(
                builder.asc(rank),
                builder.asc(startOfWhatIsNotOver),
                builder.desc(event.get("startDate")),
                builder.asc(event.get("id")));
    }

    private static Expression<Integer> statusRank(Root<EventsEntity> event, CriteriaBuilder builder) {
        CriteriaBuilder.SimpleCase<String, Integer> rank = builder.selectCase(event.<String>get("status"));
        for (int position = 0; position < RELEVANCE_ORDER.size(); position++) {
            rank = rank.when(RELEVANCE_ORDER.get(position).name(), position);
        }
        return rank.otherwise(RELEVANCE_ORDER.size());
    }

    /** A typed % or _ is a letter to look for, not a wildcard. */
    private static String escapeLikeWildcards(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
