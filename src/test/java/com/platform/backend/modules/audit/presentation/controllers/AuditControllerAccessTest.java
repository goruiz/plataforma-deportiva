package com.platform.backend.modules.audit.presentation.controllers;

import com.platform.backend.shared.infraestructure.security.AccessRules;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The audit shows who did what in the data of every organization, so only the platform owner may read it. The rule is
 * an annotation, which nothing else exercises, so a test keeps it from being loosened by accident.
 */
class AuditControllerAccessTest {

    @Test
    void onlyThePlatformOwnerMayReadTheAudit() {
        PreAuthorize rule = AuditController.class.getAnnotation(PreAuthorize.class);

        assertThat(rule).isNotNull();
        assertThat(rule.value()).isEqualTo(AccessRules.PLATFORM_OWNER_ONLY);
    }
}
