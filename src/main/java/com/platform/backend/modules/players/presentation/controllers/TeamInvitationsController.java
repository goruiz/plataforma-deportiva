package com.platform.backend.modules.players.presentation.controllers;

import com.platform.backend.modules.players.application.iservices.ITeamInvitationsService;
import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.players.presentation.responses.TeamInvitationResponse.TeamInvitationResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Team invitations", description = "Invitations to join a team, answered by the invited person")
@RestController
@RequestMapping("/team-invitations")
@RequiredArgsConstructor
public class TeamInvitationsController {

    private final ITeamInvitationsService teamInvitationsService;

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> getMine() {
        return ResponseEntity.ok(ApiResponse.ok(teamInvitationsService.getMyPendingInvitations()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<AcceptedInvitationResponse>> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(teamInvitationsService.accept(id)));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<Void>> decline(@PathVariable UUID id) {
        teamInvitationsService.decline(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
