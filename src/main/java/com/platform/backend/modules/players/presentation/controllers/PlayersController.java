package com.platform.backend.modules.players.presentation.controllers;

import com.platform.backend.modules.players.application.iservices.IPlayersService;
import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlayerResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlayerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getById(id)));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<ApiResponse<List<PlayerResponse>>> getByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.getByTeamId(teamId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlayerResponse>> search(
            @RequestParam @NotBlank @Email String email) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.searchByEmail(email)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlayerResponse>> create(@Valid @RequestBody CreatePlayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(playersService.create(request)));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<PlayerResponse>> register(@Valid @RequestBody PlayerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(playersService.registerWithInvite(request)));
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlayerResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdatePlayerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(playersService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        playersService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        playersService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
