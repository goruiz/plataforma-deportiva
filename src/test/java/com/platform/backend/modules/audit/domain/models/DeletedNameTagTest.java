package com.platform.backend.modules.audit.domain.models;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeletedNameTagTest {

    @Test
    void stripRemovesTheTagADeletionAppended() {
        String deletedName = "Liga Norte_DELETED_" + UUID.randomUUID();

        assertEquals("Liga Norte", DeletedNameTag.strip(deletedName));
    }

    @Test
    void stripAlsoCleansAnEmailOfADeletedAccount() {
        String deletedEmail = "ana@correo.com_DELETED_" + UUID.randomUUID();

        assertEquals("ana@correo.com", DeletedNameTag.strip(deletedEmail));
    }

    @Test
    void stripLeavesAnyOtherTextAsItIs() {
        assertEquals("Liga Norte", DeletedNameTag.strip("Liga Norte"));
        assertEquals("Un_DELETED_ en medio", DeletedNameTag.strip("Un_DELETED_ en medio"));
        assertEquals("", DeletedNameTag.strip(""));
        assertNull(DeletedNameTag.strip(null));
    }

    @Test
    void auditEntryShowsItsLabelAndContextWithoutTheTag() {
        UUID id = UUID.randomUUID();
        AuditEntry entry = new AuditEntry(null, id, null, null, null, null,
                "Halcones_DELETED_" + id, "Liga_DELETED_" + id, null, null, null);

        assertEquals("Halcones", entry.label());
        assertEquals("Liga", entry.context());
    }
}
