package com.platform.backend.modules.audit.presentation.controllers;

import com.platform.backend.modules.audit.application.iservices.IAuditService;
import com.platform.backend.modules.audit.domain.enums.AuditAction;
import com.platform.backend.modules.audit.domain.enums.AuditEntityType;
import com.platform.backend.modules.audit.domain.exception.AuditError;
import com.platform.backend.modules.audit.domain.filters.AuditFilter;
import com.platform.backend.modules.audit.presentation.responses.AuditEntryResponse.AuditEntryResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.pagination.ListQueryResolver;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.requests.ListRequest.ListRequest;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Tag(name = "Audit", description = "Who created, changed or deleted what (the platform owner only)")
@RestController
@RequestMapping("/audit")
@PreAuthorize(AccessRules.PLATFORM_OWNER_ONLY)
@RequiredArgsConstructor
public class AuditController {

    private static final String FROM_FIELD = "from";
    private static final String TO_FIELD = "to";

    private final IAuditService auditService;
    private final ListQueryResolver listQueryResolver;

    /**
     * Every change of every organization, the most recent first. Only the platform owner reads it: it shows who did what
     * in all the customers' data, so an administrator of one organization never sees it. {@code search} (every word must
     * appear), {@code entityType}, {@code action} and the days {@code from} and {@code to} (yyyy-MM-dd, both included)
     * narrow it.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditEntryResponse>>> search(
            ListRequest request,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LocalDate firstDay = parseDay(from, FROM_FIELD);
        LocalDate lastDay = parseDay(to, TO_FIELD);
        if (firstDay != null && lastDay != null && lastDay.isBefore(firstDay)) {
            throw new FieldValidationException(TO_FIELD, AuditError.DATE_RANGE_INVERTED.name());
        }
        AuditFilter filter = new AuditFilter(
                listQueryResolver.searchTerms(request.getSearch()),
                parseEntityType(entityType),
                parseAction(action),
                firstDay == null ? null : firstDay.atStartOfDay(),
                endOfDay(lastDay));
        PageQuery pageQuery = listQueryResolver.pageQuery(request.getPage(), request.getSize());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(auditService.search(filter, pageQuery))));
    }

    private static AuditEntityType parseEntityType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return AuditEntityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new FieldValidationException("entityType", AuditError.INVALID_ENTITY_TYPE.name());
        }
    }

    private static AuditAction parseAction(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return AuditAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new FieldValidationException("action", AuditError.INVALID_ACTION.name());
        }
    }

    private static LocalDate parseDay(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException invalid) {
            throw new FieldValidationException(field, AuditError.INVALID_DATE.name());
        }
    }

    /** The last day is included, so the limit is the start of the next one. */
    private static LocalDateTime endOfDay(LocalDate lastDay) {
        return lastDay == null ? null : lastDay.plusDays(1).atStartOfDay();
    }
}
