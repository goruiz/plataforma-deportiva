package com.platform.backend.modules.players.presentation.responses.PlayerPhotoResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

/** The image itself, as the photo endpoint sends it (the only answer of the API that is not an {@code ApiResponse}). */
@Data
@AllArgsConstructor
public class PlayerPhotoResponse {

    private String contentType;
    private byte[] data;
}
