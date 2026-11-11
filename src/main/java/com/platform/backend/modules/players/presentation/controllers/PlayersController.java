package com.platform.backend.modules.players.presentation.controllers;

import com.platform.backend.modules.players.application.iservices.IPlayersService;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.JoinByCodeRequest.JoinByCodeRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerCaptainRequest.UpdatePlayerCaptainRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerTeamAdminRequest.UpdatePlayerTeamAdminRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerStatusRequest.UpdatePlayerStatusRequest;
import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Players", description = "Player management")
@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
@Validated
public class PlayersController {

    private final IPlayersService playersService;

    // Every player, with their email and phone: an organizer's list, not something a player of the app browses.
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlayerResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getAll()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PlayerResponse>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getCurrent()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlayerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getById(id)));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<ApiResponse<List<PlayerResponse>>> getByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getByTeamId(teamId)));
    }

    // Looking a person up by email tells whether they are registered: only for whoever invites players.
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlayerResponse>> search(
            @RequestParam @NotBlank @Email String email) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.searchByEmail(email)));
    }

    // Managing the people of a team (creating, inviting, changing or deleting a player) is the organizer's work.
    // What a player does for themselves (register with a link, join with a code, leave a team) stays open:
    // leaving checks in the service that it is the caller's own membership.
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerResponse>> create(@Valid @RequestBody CreatePlayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(playersService.create(request)));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<PlayerResponse>> register(@Valid @RequestBody PlayerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(playersService.registerWithInvite(request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> invite(@Valid @RequestBody InvitePlayerRequest request) {
        playersService.invitePlayer(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .status(200)
                .message("Invitation sent")
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PostMapping("/invite-register")
    public ResponseEntity<ApiResponse<Void>> inviteRegister(@Valid @RequestBody InviteRegisterRequest request) {
        playersService.inviteRegister(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .status(200)
                .message("Registration link sent")
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    /** The signed-in player joins the team that has the code, like accepting an invitation (same answer, new token). */
    @PostMapping("/join-by-code")
    public ResponseEntity<ApiResponse<AcceptedInvitationResponse>> joinByCode(@Valid @RequestBody JoinByCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.joinByCode(request)));
    }

    /** Takes the player out of one team: {@code teamId} is only needed when the caller sees them in several. */
    @PatchMapping("/{id}/remove-from-team")
    public ResponseEntity<ApiResponse<PlayerResponse>> removeFromTeam(@PathVariable UUID id,
                                                                       @RequestParam(required = false) UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.removeFromTeam(id, teamId)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PlayerResponse>> updateStatus(@PathVariable UUID id,
                                                                     @Valid @RequestBody UpdatePlayerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.updateStatus(id, request)));
    }

    // Only an administrator designates the captain: the team's players must not be able to name themselves.
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PatchMapping("/{id}/captain")
    public ResponseEntity<ApiResponse<PlayerResponse>> updateCaptain(@PathVariable UUID id,
                                                                      @Valid @RequestBody UpdatePlayerCaptainRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.updateCaptain(id, request)));
    }

    // Only an administrator names a team's administrators, for the same reason as the captain.
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PatchMapping("/{id}/team-admin")
    public ResponseEntity<ApiResponse<PlayerResponse>> updateTeamAdmin(@PathVariable UUID id,
                                                                        @Valid @RequestBody UpdatePlayerTeamAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.updateTeamAdmin(id, request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlayerResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdatePlayerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.update(id, request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        playersService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        playersService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
