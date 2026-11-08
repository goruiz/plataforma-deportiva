package com.platform.backend.modules.players.application.iservices;

import com.platform.backend.modules.players.presentation.responses.PlayerPhotoResponse.PlayerPhotoResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;

import java.util.Optional;
import java.util.UUID;

/** The profile photo of a player, kept in the database. Only a player changes their own. */
public interface IPlayerPhotosService {

    /**
     * Saves (or replaces) the photo of the signed-in player after checking it: the file must have bytes, not exceed the
     * configured size, and be one of the allowed image types both by what the client declares and by its first bytes.
     * Answers with the player, whose {@code profilePhotoUrl} now points to the new photo.
     */
    PlayerResponse saveCurrentPlayerPhoto(byte[] data, String declaredContentType);

    /** Erases the photo of the signed-in player and clears their {@code profilePhotoUrl}. Safe to repeat. */
    PlayerResponse deleteCurrentPlayerPhoto();

    /**
     * The photo of a player the caller can see; empty both when the player has none and when the caller cannot see the
     * player, so the answer does not tell them apart.
     */
    Optional<PlayerPhotoResponse> findPhoto(UUID playerId);
}
