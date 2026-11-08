package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.mappers.PlayerMapper;
import com.platform.backend.modules.players.domain.entities.PlayerPhotosEntity;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.exception.PlayerPhotoError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.presentation.responses.PlayerPhotoResponse.PlayerPhotoResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerPhotosServiceTest {

    private static final String EMAIL = "Juan@Email.com";
    private static final int MAX_BYTES = 100;

    @Mock private IPlayerRepository playerRepository;
    @Mock private IPlayerPhotoRepository photoRepository;
    @Mock private PlayerMapper playerMapper;

    private PlayerPhotosService playerPhotosService;
    private PlayersEntity player;

    @BeforeEach
    void setUp() {
        playerPhotosService = new PlayerPhotosService(playerRepository, photoRepository, playerMapper, MAX_BYTES,
                List.of("image/jpeg", "image/png", "IMAGE/WEBP"));

        player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setEmail("juan@email.com");
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), EMAIL);

        lenient().when(playerRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(player));
        lenient().when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(playerMapper.toResponse(any(PlayersEntity.class))).thenAnswer(invocation -> {
            PlayerResponse response = new PlayerResponse();
            response.setProfilePhotoUrl(((PlayersEntity) invocation.getArgument(0)).getProfilePhotoUrl());
            return response;
        });
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static byte[] jpeg(int size) {
        byte[] data = new byte[size];
        data[0] = (byte) 0xFF;
        data[1] = (byte) 0xD8;
        data[2] = (byte) 0xFF;
        return data;
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    }

    private static void assertRefusedWith(Runnable action, PlayerPhotoError reason) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(FieldValidationException.class, exception ->
                assertThat(exception.getFieldErrors()).containsOnly(entry("file", reason.name())));
    }

    // ─── save ───────────────────────────────────────────────────────────────────

    @Test
    void saveCurrentPlayerPhoto_whenThePhotoIsValid_thenItIsStoredAndTheProfileUrlPointsToIt() {
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.empty());
        byte[] data = jpeg(40);

        PlayerResponse response = playerPhotosService.saveCurrentPlayerPhoto(data, "image/jpeg");

        ArgumentCaptor<PlayerPhotosEntity> saved = ArgumentCaptor.forClass(PlayerPhotosEntity.class);
        verify(photoRepository).save(saved.capture());
        assertThat(saved.getValue().getPlayerId()).isEqualTo(player.getId());
        assertThat(saved.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(40);
        assertThat(saved.getValue().getData()).isEqualTo(data);
        assertThat(response.getProfilePhotoUrl()).matches("players/" + player.getId() + "/photo\\?v=\\d+");
    }

    @Test
    void saveCurrentPlayerPhoto_whenThePlayerAlreadyHasOne_thenTheSameRowIsRewritten() {
        PlayerPhotosEntity existing = new PlayerPhotosEntity();
        existing.setId(UUID.randomUUID());
        existing.setPlayerId(player.getId());
        existing.setContentType("image/jpeg");
        existing.setData(jpeg(10));
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.of(existing));

        playerPhotosService.saveCurrentPlayerPhoto(png(), "image/png");

        verify(photoRepository).save(existing);
        assertThat(existing.getContentType()).isEqualTo("image/png");
        assertThat(existing.getSizeBytes()).isEqualTo(10);
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheDeclaredTypeHasParametersOrCapitals_thenItIsStillTheSameType() {
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.empty());

        playerPhotosService.saveCurrentPlayerPhoto(jpeg(20), "Image/JPEG; charset=binary");

        verify(photoRepository).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheAllowedListUsesCapitals_thenItStillMatches() {
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.empty());
        byte[] webp = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

        playerPhotosService.saveCurrentPlayerPhoto(webp, "image/webp");

        verify(photoRepository).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenNoBytesCame_thenEmptyFile() {
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto(null, "image/jpeg"), PlayerPhotoError.EMPTY_FILE);
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto(new byte[0], "image/jpeg"), PlayerPhotoError.EMPTY_FILE);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheFileIsBiggerThanTheLimit_thenFileTooLarge() {
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto(jpeg(MAX_BYTES + 1), "image/jpeg"),
                PlayerPhotoError.FILE_TOO_LARGE);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheFileHasExactlyTheLimit_thenItIsAccepted() {
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.empty());

        playerPhotosService.saveCurrentPlayerPhoto(jpeg(MAX_BYTES), "image/jpeg");

        verify(photoRepository).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheDeclaredTypeIsNotAllowed_thenUnsupportedType() {
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto(jpeg(20), "image/gif"), PlayerPhotoError.UNSUPPORTED_TYPE);
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto(jpeg(20), null), PlayerPhotoError.UNSUPPORTED_TYPE);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheBytesAreNotTheDeclaredImage_thenUnsupportedType() {
        // A PNG sent as a JPEG, and plain text sent as a JPEG: the first bytes decide, not the header.
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto(png(), "image/jpeg"), PlayerPhotoError.UNSUPPORTED_TYPE);
        assertRefusedWith(() -> playerPhotosService.saveCurrentPlayerPhoto("<script>".getBytes(), "image/jpeg"),
                PlayerPhotoError.UNSUPPORTED_TYPE);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void saveCurrentPlayerPhoto_whenTheAccountHasNoPlayer_thenNotFound() {
        when(playerRepository.findByEmail("juan@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerPhotosService.saveCurrentPlayerPhoto(jpeg(20), "image/jpeg"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void saveCurrentPlayerPhoto_whenThePlayerWasDeleted_thenNotFound() {
        player.setDeletedAt(LocalDateTime.now());

        assertThatThrownBy(() -> playerPhotosService.saveCurrentPlayerPhoto(jpeg(20), "image/jpeg"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void deleteCurrentPlayerPhoto_whenCalled_thenTheBytesAreErasedAndTheProfileUrlIsCleared() {
        player.setProfilePhotoUrl("players/" + player.getId() + "/photo?v=1");

        PlayerResponse response = playerPhotosService.deleteCurrentPlayerPhoto();

        verify(photoRepository).deleteByPlayerId(player.getId());
        assertThat(player.getProfilePhotoUrl()).isNull();
        assertThat(response.getProfilePhotoUrl()).isNull();
    }

    // ─── read ───────────────────────────────────────────────────────────────────

    @Test
    void findPhoto_whenThePlayerIsVisibleAndHasAPhoto_thenItsTypeAndBytes() {
        PlayerPhotosEntity photo = new PlayerPhotosEntity();
        photo.setContentType("image/png");
        photo.setData(png());
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.of(photo));

        PlayerPhotoResponse response = playerPhotosService.findPhoto(player.getId()).orElseThrow();

        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getData()).isEqualTo(png());
    }

    @Test
    void findPhoto_whenThePlayerIsNotVisibleToTheCaller_thenNothingAndTheBytesAreNotEvenLookedFor() {
        UUID otherOrganizationsPlayer = UUID.randomUUID();
        when(playerRepository.findActiveById(otherOrganizationsPlayer)).thenReturn(Optional.empty());

        assertThat(playerPhotosService.findPhoto(otherOrganizationsPlayer)).isEmpty();
        verify(photoRepository, never()).findByPlayerId(any());
    }

    @Test
    void findPhoto_whenThePlayerHasNoPhoto_thenNothing() {
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));
        when(photoRepository.findByPlayerId(player.getId())).thenReturn(Optional.empty());

        assertThat(playerPhotosService.findPhoto(player.getId())).isEmpty();
    }
}
