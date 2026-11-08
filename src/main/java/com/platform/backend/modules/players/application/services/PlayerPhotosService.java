package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.IPlayerPhotosService;
import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayerPhotosEntity;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.enums.PhotoFormat;
import com.platform.backend.modules.players.domain.exception.PlayerPhotoError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.responses.PlayerPhotoResponse.PlayerPhotoResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The photo lives in {@code player_photos}, and the player's {@code profilePhotoUrl} points to it with a relative path
 * (the client resolves it against the API address), so every list that already carries that URL shows the photo.
 * <p>
 * A photo is only reachable through its player, and a player is looked up with the organization filter: a photo of a
 * player the caller cannot see is a 404, like the player itself.
 */
@Service
public class PlayerPhotosService implements IPlayerPhotosService {

    private static final String FILE_FIELD = "file";

    /**
     * Relative to the API address. The version is the upload time: a new photo is a new URL, so a client that cached the
     * old one (the photo itself is cacheable) fetches the new one.
     */
    private static final String PHOTO_URL_FORMAT = "players/%s/photo?v=%d";

    private final IPlayerRepository playerRepository;
    private final IPlayerPhotoRepository photoRepository;
    private final PlayerMapper playerMapper;
    private final int maxBytes;
    private final Set<String> allowedContentTypes;

    public PlayerPhotosService(IPlayerRepository playerRepository,
                               IPlayerPhotoRepository photoRepository,
                               PlayerMapper playerMapper,
                               @Value("${app.photos.max-bytes:2097152}") int maxBytes,
                               @Value("${app.photos.allowed-content-types:image/jpeg,image/png,image/webp}")
                               List<String> allowedContentTypes) {
        this.playerRepository = playerRepository;
        this.photoRepository = photoRepository;
        this.playerMapper = playerMapper;
        this.maxBytes = maxBytes;
        this.allowedContentTypes = allowedContentTypes.stream().map(PlayerPhotosService::normalizeContentType)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public PlayerResponse saveCurrentPlayerPhoto(byte[] data, String declaredContentType) {
        PlayersEntity player = findCurrentPlayer();
        PhotoFormat format = requireAllowedImage(data, declaredContentType);

        PlayerPhotosEntity photo = photoRepository.findByPlayerId(player.getId()).orElseGet(PlayerPhotosEntity::new);
        photo.setPlayerId(player.getId());
        photo.setContentType(format.contentType());
        photo.setSizeBytes(data.length);
        photo.setData(data);
        photoRepository.save(photo);

        player.setProfilePhotoUrl(PHOTO_URL_FORMAT.formatted(player.getId(), Instant.now().toEpochMilli()));
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public PlayerResponse deleteCurrentPlayerPhoto() {
        PlayersEntity player = findCurrentPlayer();
        photoRepository.deleteByPlayerId(player.getId());
        player.setProfilePhotoUrl(null);
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerPhotoResponse> findPhoto(UUID playerId) {
        return playerRepository.findActiveById(playerId)
                .flatMap(player -> photoRepository.findByPlayerId(player.getId()))
                .map(photo -> new PlayerPhotoResponse(photo.getContentType(), photo.getData()));
    }

    /** The account that is calling, as a player; whoever is not one (an administrator) has no photo to change. */
    private PlayersEntity findCurrentPlayer() {
        String email = EmailNormalizer.normalize(CurrentCaller.require().getEmail());
        return playerRepository.findByEmail(email)
                .filter(player -> player.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));
    }

    /**
     * Checks the file in the order a client can fix it: something was sent, it is small enough, and it really is the
     * image type it says it is (the first bytes decide; the declared type can be anything) and an allowed one.
     */
    private PhotoFormat requireAllowedImage(byte[] data, String declaredContentType) {
        if (data == null || data.length == 0) {
            throw refused(PlayerPhotoError.EMPTY_FILE);
        }
        if (data.length > maxBytes) {
            throw refused(PlayerPhotoError.FILE_TOO_LARGE);
        }
        PhotoFormat format = PhotoFormat.detect(data).orElseThrow(() -> refused(PlayerPhotoError.UNSUPPORTED_TYPE));
        String declared = normalizeContentType(declaredContentType);
        if (!allowedContentTypes.contains(declared) || !format.contentType().equals(declared)) {
            throw refused(PlayerPhotoError.UNSUPPORTED_TYPE);
        }
        return format;
    }

    /** {@code Image/JPEG; charset=x} and {@code image/jpeg} are the same type. */
    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private static FieldValidationException refused(PlayerPhotoError reason) {
        return new FieldValidationException(FILE_FIELD, reason.name());
    }
}
