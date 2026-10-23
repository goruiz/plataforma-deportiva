package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.filters.UserListFilter;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The list of active accounts as a JPA query. It is built with the criteria API (not a native query) on purpose: only
 * entity queries go through the organization filter, so a native one would show every organization's accounts to an
 * administrator of just one.
 */
public final class UserSpecifications {

    /**
     * The database function {@code users_search_text(...)} created by migration V8: the lower-cased name, username and
     * email joined in one text. Searching through it lets a single trigram index serve every word of a search.
     */
    private static final String SEARCH_TEXT_FUNCTION = "users_search_text";
    private static final char LIKE_ESCAPE = '\\';

    private UserSpecifications() {}

    public static Specification<UsersEntity> matching(UserListFilter filter) {
        return (user, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(builder.isNull(user.get("deletedAt")));
            if (!filter.withAnyRoleIds().isEmpty()) {
                conditions.add(builder.exists(roleAssignment(user, query, builder, filter.withAnyRoleIds())));
            }
            if (!filter.withoutAnyRoleIds().isEmpty()) {
                conditions.add(builder.not(builder.exists(roleAssignment(user, query, builder, filter.withoutAnyRoleIds()))));
            }
            conditions.addAll(containsEveryTerm(user, builder, filter.searchTerms()));
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }

    /** Active assignments of the account to any of the roles (the inside of an EXISTS). */
    private static Subquery<Integer> roleAssignment(
            Root<UsersEntity> user, AbstractQuery<?> query, CriteriaBuilder builder, Collection<UUID> roleIds) {
        Subquery<Integer> assignment = query.subquery(Integer.class);
        Root<UsersRolesEntity> userRole = assignment.from(UsersRolesEntity.class);
        return assignment.select(builder.literal(1)).where(
                builder.equal(userRole.get("user").get("id"), user.get("id")),
                userRole.get("role").get("id").in(roleIds),
                builder.isNull(userRole.get("deletedAt")));
    }

    /** One condition per word, all of them required: "ana perez" finds the account of Ana Pérez, not every Ana or Perez. */
    private static List<Predicate> containsEveryTerm(Root<UsersEntity> user, CriteriaBuilder builder, List<String> terms) {
        if (terms.isEmpty()) {
            return List.of();
        }
        Expression<String> searchText = builder.function(
                SEARCH_TEXT_FUNCTION, String.class,
                user.get("firstName"), user.get("middleName"), user.get("lastName"),
                user.get("secondLastName"), user.get("username"), user.get("email"));
        return terms.stream()
                .map(term -> builder.like(searchText, "%" + escapeLikeWildcards(term) + "%", LIKE_ESCAPE))
                .toList();
    }

    /** A typed % or _ is a letter to look for, not a wildcard. */
    private static String escapeLikeWildcards(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
