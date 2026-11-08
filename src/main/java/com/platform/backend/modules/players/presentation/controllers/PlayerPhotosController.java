package com.platform.backend.modules.players.presentation.controllers;

import com.platform.backend.modules.players.application.iservices.IPlayerPhotosService;
import com.platform.backend.modules.players.presentation.responses.PlayerPhotoResponse.PlayerPhotoResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.UUID;

@Tag(name = "Player photos", description = "Profile photo of a player")
@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerPhotosController {

    private final IPlayerPhotosService playerPhotosService;

    // The URL of a photo carries its version, so it can be cached for long; a client that changes the photo gets a new URL.
    @Value("${app.photos.cache-max-age-seconds:86400}")
    private long cacheMaxAgeSeconds;

    /** The signed-in player uploads their photo as the multipart part {@code file}. */
    @PutMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PlayerResponse>> save(
            @RequestPart(name = "file", required = false) MultipartFile file) {
        byte[] data = file == null ? null : readBytes(file);
        String declaredContentType = file == null ? null : file.getContentType();
        return ResponseEntity.ok(ApiResponse.ok(playerPhotosService.saveCurrentPlayerPhoto(data, declaredContentType)));
    }

    @DeleteMapping("/me/photo")
    public ResponseEntity<ApiResponse<PlayerResponse>> delete() {
        return ResponseEntity.ok(ApiResponse.ok(playerPhotosService.deleteCurrentPlayerPhoto()));
    }

    /**
     * The image itself: the one answer of the API that is not wrapped in an {@code ApiResponse}. When there is nothing to
     * show the 404 has no body: a client that asked for an image ({@code Accept: image/*}) cannot be sent a JSON error,
     * and the error dispatch of a refused negotiation would turn that 404 into a 401.
     */
    @GetMapping("/{playerId}/photo")
    public ResponseEntity<byte[]> get(@PathVariable UUID playerId) {
        return playerPhotosService.findPhoto(playerId)
                .map(this::toImageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> toImageResponse(PlayerPhotoResponse photo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(cacheMaxAgeSeconds)).cachePrivate())
                .body(photo.getData());
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("The uploaded photo could not be read", e);
        }
    }
}
