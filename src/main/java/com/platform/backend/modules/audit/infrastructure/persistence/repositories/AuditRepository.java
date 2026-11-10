package com.platform.backend.modules.audit.infrastructure.persistence.repositories;

import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;
import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.modules.audit.domain.irepositories.IAuditRepository;
import com.platform.backend.modules.audit.domain.models.AuditEntry;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditRepository implements IAuditRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PageResult<AuditEntry> search(AuditFilter filter, PageQuery pageQuery) {
        AuditQuery auditQuery = AuditQuery.of(filter, CurrentTenant.scope());
        long total = countEntries(auditQuery);
        if (total == 0 || pageQuery.offset() >= total) {
            return new PageResult<>(List.of(), total, pageQuery);
        }
        return new PageResult<>(readPage(auditQuery, pageQuery), total, pageQuery);
    }

    private long countEntries(AuditQuery auditQuery) {
        Query query = entityManager.createNativeQuery(auditQuery.countSql());
        bindParameters(query, auditQuery);
        return ((Number) query.getSingleResult()).longValue();
    }

    private List<AuditEntry> readPage(AuditQuery auditQuery, PageQuery pageQuery) {
        Query query = entityManager.createNativeQuery(auditQuery.selectSql());
        bindParameters(query, auditQuery);
        query.setFirstResult((int) pageQuery.offset());
        query.setMaxResults(pageQuery.size());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(AuditRepository::toEntry).toList();
    }

    private static void bindParameters(Query query, AuditQuery auditQuery) {
        auditQuery.parameters().forEach(query::setParameter);
    }

    /** One row, in the order of {@code AuditQuery.SELECT_COLUMNS}. */
    private static AuditEntry toEntry(Object[] row) {
        return new AuditEntry(
                AuditEntityType.valueOf((String) row[0]),
                (UUID) row[1],
                AuditAction.valueOf((String) row[2]),
                toLocalDateTime(row[3]),
                (UUID) row[4],
                (String) row[5],
                (String) row[6],
                (String) row[7],
                (String) row[8],
                (UUID) row[9],
                (String) row[10]);
    }

    /** Hibernate hands a native timestamp over as a {@link Timestamp} or as a {@link LocalDateTime}, by version. */
    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime moment) {
            return moment;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
