package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.application.iservices.ITeamMembershipsService;
import com.platform.backend.modules.players.domain.exception.PlayerError;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
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
    @Mock private ITeamMemberRepository teamMemberRepository;
    @Mock private ITeamMembershipsService teamMembershipsService;

    @InjectMocks
    private PlayersService playersService;

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    @Test
    void delete_whenThePlayerExists_thenItIsSoftDeletedAndItsPhotoIsErased() {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));

        playersService.delete(player.getId());

        assertThat(player.getDeletedAt()).isNotNull();
        verify(playerRepository).save(player);
        verify(playerPhotoRepository).deleteByPlayerId(player.getId());
        verify(teamMembershipsService).leaveEvery(player);
    }

    @Test
    void delete_whenAnOrganizationAsksForAPersonWhoAlsoPlaysForAnother_thenRefusedAndNothingIsErased() {
        UUID organizationId = UUID.randomUUID();
        TenantTestSupport.actAsOrganizationAdmin(organizationId);
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));
        when(teamMemberRepository.existsActiveOutsideOrganization(player.getId(), organizationId)).thenReturn(true);

        assertThatThrownBy(() -> playersService.delete(player.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PlayerError.PLAYS_FOR_ANOTHER_ORGANIZATION.name());

        assertThat(player.getDeletedAt()).isNull();
        verify(teamMembershipsService, never()).leaveEvery(any());
        verify(playerPhotoRepository, never()).deleteByPlayerId(any());
    }

    @Test
    void delete_whenAnOrganizationAsksForAPersonWhoOnlyPlaysForIt_thenTheyAreErased() {
        UUID organizationId = UUID.randomUUID();
        TenantTestSupport.actAsOrganizationAdmin(organizationId);
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));
        when(teamMemberRepository.existsActiveOutsideOrganization(player.getId(), organizationId)).thenReturn(false);

        playersService.delete(player.getId());

        assertThat(player.getDeletedAt()).isNotNull();
    }

    @Test
    void delete_whenThePlatformOwnerAsks_thenThePersonIsErasedWhateverTeamsTheyHave() {
        TenantTestSupport.actAsPlatformOwner();
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        when(playerRepository.findActiveById(player.getId())).thenReturn(Optional.of(player));

        playersService.delete(player.getId());

        assertThat(player.getDeletedAt()).isNotNull();
        verify(teamMemberRepository, never()).existsActiveOutsideOrganization(any(), any());
    }

    @Test
    void delete_whenThePlayerDoesNotExist_thenNotFoundAndNothingIsErased() {
        UUID unknown = UUID.randomUUID();
        when(playerRepository.findActiveById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playersService.delete(unknown)).isInstanceOf(EntityNotFoundException.class);
        verify(playerPhotoRepository, never()).deleteByPlayerId(any());
    }
}
