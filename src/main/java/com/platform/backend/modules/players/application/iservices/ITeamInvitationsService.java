package com.platform.backend.modules.players.application.iservices;

import com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse.AcceptedInvitationResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.players.presentation.responses.TeamInvitationResponse.TeamInvitationResponse;

import java.util.List;
import java.util.UUID;

/** The invitations to join a team, seen by the person who was invited. */
public interface ITeamInvitationsService {

    /** The caller's invitations that can still be answered. */
    List<TeamInvitationResponse> getMyPendingInvitations();

    /** Puts the caller's player in the team that invited them, and gives them a session for the organization of that team. */
    AcceptedInvitationResponse accept(UUID invitationId);

    void decline(UUID invitationId);
}
