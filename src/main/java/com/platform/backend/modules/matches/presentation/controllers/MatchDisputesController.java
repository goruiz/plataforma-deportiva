package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IMatchDisputesService;
import com.platform.backend.modules.matches.domain.enums.DisputeStatus;
import com.platform.backend.modules.matches.domain.exception.DisputeError;
import com.platform.backend.modules.matches.domain.filters.DisputeFilter;
import com.platform.backend.modules.matches.presentation.requests.OpenDisputeRequest.OpenDisputeRequest;
import com.platform.backend.modules.matches.presentation.requests.ResolveDisputeRequest.ResolveDisputeRequest;
import com.platform.backend.modules.matches.presentation.responses.DisputeCountsResponse.DisputeCountsResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchDisputeResponse.MatchDisputeResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.pagination.ListQueryResolver;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.requests.ListRequest.ListRequest;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Match disputes", description = "A team disputes the result of a match; an administrator decides")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class MatchDisputesController {

    private static final String STATUS_FIELD = "status";

    private final IMatchDisputesService disputesService;
    private final ListQueryResolver listQueryResolver;

    /** The captain of a team (or an administrator) disputes the result of a match on the team's behalf. */
    @PostMapping("/matches/{matchId}/disputes")
    public ResponseEntity<ApiResponse<MatchDisputeResponse>> open(@PathVariable UUID matchId,
                                                                  @Valid @RequestBody OpenDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(disputesService.open(matchId, request)));
    }

    @GetMapping("/matches/{matchId}/disputes")
    public ResponseEntity<ApiResponse<List<MatchDisputeResponse>>> getByMatch(@PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponse.ok(disputesService.getByMatch(matchId)));
    }

    /** The disputes the caller may see, the most recent first; {@code search} (every word) and {@code status} narrow it. */
    @GetMapping("/disputes")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<PageResponse<MatchDisputeResponse>>> search(
            ListRequest request, @RequestParam(required = false) String status) {
        DisputeFilter filter = new DisputeFilter(listQueryResolver.searchTerms(request.getSearch()), parseStatus(status));
        PageQuery pageQuery = listQueryResolver.pageQuery(request.getPage(), request.getSize());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(disputesService.search(filter, pageQuery))));
    }

    /** How many disputes each state has for the same {@code search}, for the numbers on the filters. */
    @GetMapping("/disputes/counts")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<DisputeCountsResponse>> counts(@RequestParam(required = false) String search) {
        DisputeFilter filter = new DisputeFilter(listQueryResolver.searchTerms(search), null);
        return ResponseEntity.ok(ApiResponse.ok(disputesService.countByStatus(filter)));
    }

    /** The disputes of the teams the caller plays for (the mobile app). */
    @GetMapping("/disputes/mine")
    public ResponseEntity<ApiResponse<List<MatchDisputeResponse>>> getMine() {
        return ResponseEntity.ok(ApiResponse.ok(disputesService.getMine()));
    }

    /** Administrator or the creator of the event: the service checks, so the account that created a league can answer. */
    @PatchMapping("/disputes/{id}/resolve")
    public ResponseEntity<ApiResponse<MatchDisputeResponse>> resolve(@PathVariable UUID id,
                                                                    @Valid @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(disputesService.resolve(id, request)));
    }

    private static DisputeStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return DisputeStatus.parse(value)
                .orElseThrow(() -> new FieldValidationException(STATUS_FIELD, DisputeError.INVALID_STATUS.name()));
    }
}
