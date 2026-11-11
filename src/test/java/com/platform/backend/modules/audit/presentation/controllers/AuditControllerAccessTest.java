package com.platform.backend.modules.audit.presentation.controllers;

import com.platform.backend.shared.infraestructure.security.AccessRules;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The audit is per administrator: any administrator reads the trail of their own organization (the query itself is
 * narrowed to it) and the platform owner reads every organization's. What nobody else may do is read it at all, so the
 * rule (an annotation, which nothing else exercises) is pinned here: it must not be loosened to any account, nor
 * tightened back to the owner only.
 */
class AuditControllerAccessTest {

    @Test
    void onlyAdministratorsMayReadTheAudit() {
        PreAuthorize rule = AuditController.class.getAnnotation(PreAuthorize.class);

        assertThat(rule).isNotNull();
        assertThat(rule.value()).isEqualTo(AccessRules.ADMIN_ONLY);
    }
}
