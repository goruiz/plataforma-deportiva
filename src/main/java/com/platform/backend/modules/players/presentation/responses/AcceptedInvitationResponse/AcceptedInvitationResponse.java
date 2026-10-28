package com.platform.backend.modules.players.presentation.responses.AcceptedInvitationResponse;

import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * What accepting an invitation answers: the player already in the team and a new session token. The token carries the
 * organization of the account, which is now the team's, so the app has to replace the one it holds to see its data.
 */
@Data
@AllArgsConstructor
public class AcceptedInvitationResponse {

    private PlayerResponse player;
    private String token;
}
