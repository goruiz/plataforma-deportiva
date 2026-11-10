package com.platform.backend.modules.players.application.iservices;

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

import java.util.List;
import java.util.UUID;

public interface IPlayersService {

    List<PlayerResponse> getAll();

    PlayerResponse getById(UUID id);

    PlayerResponse searchByEmail(String email);

    /** The player that goes with the account that is calling (matched by email). */
    PlayerResponse getCurrent();

    PlayerResponse create(CreatePlayerRequest request);

    PlayerResponse registerWithInvite(PlayerRegisterRequest request);

    PlayerResponse update(UUID id, UpdatePlayerRequest request);

    /**
     * Puts the signed-in player in the team that has that code, like accepting an invitation: the answer carries the
     * new session token, because the organization of the account is now the team's. Needs a confirmed email, and a
     * player without a team.
     */
    AcceptedInvitationResponse joinByCode(JoinByCodeRequest request);

    void invitePlayer(InvitePlayerRequest request);

    void inviteRegister(InviteRegisterRequest request);

    List<PlayerResponse> getByTeamId(UUID teamId);

    /**
     * The player stops playing for one team (the others stay). {@code teamId} is optional when the caller sees the
     * player in a single team, which is always the case for an administrator of an organization.
     */
    PlayerResponse removeFromTeam(UUID id, UUID teamId);

    PlayerResponse updateStatus(UUID id, UpdatePlayerStatusRequest request);

    /** Designates (or removes) the captain of the player's team; there is at most one per team. */
    PlayerResponse updateCaptain(UUID id, UpdatePlayerCaptainRequest request);

    /** Makes the player an administrator of their team, or stops them being one; a team can have several. */
    PlayerResponse updateTeamAdmin(UUID id, UpdatePlayerTeamAdminRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
