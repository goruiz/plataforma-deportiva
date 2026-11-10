package com.platform.backend.modules.audit.infrastructure.persistence.repositories;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;
import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.shared.domain.tenancy.TenantScope;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditQueryTest {

    private static final AuditFilter NO_FILTER = new AuditFilter(List.of(), null, null, null, null);

    @Test
    void everyAuditedKindOfRecordHasASourceAndNoneIsRepeated() {
        assertThat(Arrays.stream(AuditSource.values()).map(AuditSource::entityType))
                .containsExactlyInAnyOrder(AuditEntityType.values());
    }

    @Test
    void withoutFiltersItReadsCreationChangeAndDeletionOfEveryTable() {
        AuditQuery query = AuditQuery.of(NO_FILTER, TenantScope.unrestricted());

        int branches = AuditSource.values().length * AuditAction.values().length;
        assertThat(occurrences(query.selectSql(), " UNION ALL ")).isEqualTo(branches - 1);
        assertThat(query.selectSql()).doesNotContain(" WHERE a.");
        assertThat(query.parameters()).isEmpty();
    }

    @Test
    void anEntityTypeAndAnActionKeepOnlyTheBranchesThatCanMatch() {
        AuditFilter filter = new AuditFilter(List.of(), AuditEntityType.TEAM, AuditAction.DELETED, null, null);

        AuditQuery query = AuditQuery.of(filter, TenantScope.unrestricted());

        assertThat(occurrences(query.selectSql(), " UNION ALL ")).isZero();
        assertThat(query.selectSql()).contains("FROM teams t").contains("t.deleted_at IS NOT NULL")
                .doesNotContain("FROM events t");
    }

    @Test
    void aChangeIsARowTouchedAfterItsCreationThatIsStillAlive() {
        AuditFilter filter = new AuditFilter(List.of(), AuditEntityType.EVENT, AuditAction.UPDATED, null, null);

        String sql = AuditQuery.of(filter, TenantScope.unrestricted()).selectSql();

        assertThat(sql).contains("t.deleted_at IS NULL AND t.updated_at > t.created_at");
    }

    @Test
    void onlyChangesWithAKnownAuthorAreListed() {
        AuditFilter filter = new AuditFilter(List.of(), AuditEntityType.EVENT, AuditAction.CREATED, null, null);

        String sql = AuditQuery.of(filter, TenantScope.unrestricted()).selectSql();

        assertThat(sql).contains("t.created_by IS NOT NULL");
    }

    @Test
    void anAdministratorOfAnOrganizationOnlySeesItsOwnRecords() {
        UUID organizationId = UUID.randomUUID();

        AuditQuery query = AuditQuery.of(NO_FILTER, TenantScope.ofOrganization(organizationId));

        assertThat(query.selectSql()).contains("a.organization_id = :organizationId");
        assertThat(query.countSql()).contains("a.organization_id = :organizationId");
        assertThat(query.parameters()).containsEntry("organizationId", organizationId);
    }

    @Test
    void thePlatformOwnerIsNotRestrictedToAnOrganization() {
        AuditQuery query = AuditQuery.of(NO_FILTER, TenantScope.unrestricted());

        assertThat(query.selectSql()).doesNotContain(":organizationId");
        assertThat(query.parameters()).doesNotContainKey("organizationId");
    }

    @Test
    void theDaysBecomeABoundedRange() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime until = LocalDateTime.of(2026, 11, 8, 0, 0);
        AuditFilter filter = new AuditFilter(List.of(), null, null, from, until);

        AuditQuery query = AuditQuery.of(filter, TenantScope.unrestricted());

        assertThat(query.selectSql()).contains("a.occurred_at >= :fromMoment").contains("a.occurred_at < :untilMoment");
        assertThat(query.parameters()).containsEntry("fromMoment", from).containsEntry("untilMoment", until);
    }

    @Test
    void everyWordIsABoundParameterAndNeverPartOfTheSql() {
        AuditFilter filter = new AuditFilter(List.of("liga", "norte'; drop table users; --"), null, null, null, null);

        AuditQuery query = AuditQuery.of(filter, TenantScope.unrestricted());

        assertThat(query.selectSql()).contains(":term0").contains(":term1")
                .doesNotContain("drop table").doesNotContain("liga");
        assertThat(query.parameters()).containsEntry("term0", "%liga%")
                .containsEntry("term1", "%norte'; drop table users; --%");
    }

    @Test
    void aTypedWildcardIsTextToFindNotAPattern() {
        AuditFilter filter = new AuditFilter(List.of("100%", "a_b", "hey!"), null, null, null, null);

        AuditQuery query = AuditQuery.of(filter, TenantScope.unrestricted());

        assertThat(query.parameters())
                .containsEntry("term0", "%100!%%")
                .containsEntry("term1", "%a!_b%")
                .containsEntry("term2", "%hey!!%");
    }

    @Test
    void theNewestChangeComesFirstAndTiesAreBrokenSoPagesNeverRepeat() {
        String sql = AuditQuery.of(NO_FILTER, TenantScope.unrestricted()).selectSql();

        assertThat(sql).endsWith("ORDER BY a.occurred_at DESC, a.entity_type, a.entity_id, "
                + "CASE a.action WHEN 'CREATED' THEN 3 WHEN 'UPDATED' THEN 2 WHEN 'DELETED' THEN 1 END");
    }

    private static int occurrences(String text, String piece) {
        return text.split(java.util.regex.Pattern.quote(piece), -1).length - 1;
    }
}
