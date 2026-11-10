package com.platform.backend.modules.audit.application.services;

import com.platform.backend.modules.audit.application.mappers.AuditEntryMapper;
import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;
import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.modules.audit.domain.irepositories.IAuditRepository;
import com.platform.backend.modules.audit.domain.models.AuditEntry;
import com.platform.backend.modules.audit.presentation.responses.AuditEntryResponse.AuditEntryResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private IAuditRepository auditRepository;

    private final AuditEntryMapper auditEntryMapper = Mappers.getMapper(AuditEntryMapper.class);

    @Test
    void itReturnsTheRepositoryPageWithEachLineAsAResponse() {
        UUID eventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LocalDateTime moment = LocalDateTime.of(2026, 11, 10, 9, 30);
        AuditFilter filter = new AuditFilter(List.of(), null, null, null, null);
        PageQuery pageQuery = new PageQuery(0, 20);
        AuditEntry entry = new AuditEntry(AuditEntityType.EVENT, eventId, AuditAction.CREATED, moment, actorId,
                "Ana Pérez", "Liga Norte", null, null, null, null);
        when(auditRepository.search(filter, pageQuery)).thenReturn(new PageResult<>(List.of(entry), 1, pageQuery));

        PageResult<AuditEntryResponse> page = new AuditService(auditRepository, auditEntryMapper).search(filter, pageQuery);

        assertThat(page.totalElements()).isEqualTo(1);
        AuditEntryResponse response = page.content().get(0);
        assertThat(response.getEntityType()).isEqualTo(AuditEntityType.EVENT);
        assertThat(response.getEntityId()).isEqualTo(eventId);
        assertThat(response.getAction()).isEqualTo(AuditAction.CREATED);
        assertThat(response.getOccurredAt()).isEqualTo(moment);
        assertThat(response.getActorId()).isEqualTo(actorId);
        assertThat(response.getActorName()).isEqualTo("Ana Pérez");
        assertThat(response.getLabel()).isEqualTo("Liga Norte");
    }
}
