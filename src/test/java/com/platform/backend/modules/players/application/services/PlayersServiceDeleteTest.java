package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayersServiceDeleteTest {

    @Mock private IPlayerRepository playerRepository;
    @Mock private IPlayerPhotoRepository playerPhotoRepository;

    @InjectMocks
    private PlayersService playersService;

    @Test
    void delete_whenThePlayerExists_thenItIsSoftDeletedAndItsPhotoIsErased() {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));

        playersService.delete(player.getId());

        assertThat(player.getDeletedAt()).isNotNull();
        verify(playerRepository).save(player);
        verify(playerPhotoRepository).deleteByPlayerId(player.getId());
    }

    @Test
    void delete_whenThePlayerDoesNotExist_thenNotFoundAndNothingIsErased() {
        UUID unknown = UUID.randomUUID();
        when(playerRepository.findActiveById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.delete(unknown)).isInstanceOf(EntityNotFoundException.class);
        verify(playerPhotoRepository, never()).deleteByPlayerId(any());
    }
}
