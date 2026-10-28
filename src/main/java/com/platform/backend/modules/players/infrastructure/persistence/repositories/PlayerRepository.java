package com.platform.backend.modules.players.infrastructure.persistence.repositories;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.infrastructure.persistence.jpa.PlayersJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PlayerRepository implements IPlayerRepository {

    private final PlayersJpaRepository playersJpaRepository;

    @Override
    public PlayersEntity save(PlayersEntity player) {
        return playersJpaRepository.save(player);
    }

    @Override
    public Optional<PlayersEntity> findActiveById(UUID id) {
        return playersJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<PlayersEntity> findAllActiveByIds(Collection<UUID> ids) {
        return playersJpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Map<UUID, String> findFullNamesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return playersJpaRepository.findNamesByIds(ids).stream().collect(Collectors.toMap(
                PlayersJpaRepository.PlayerName::getId,
                player -> (player.getFirstName() + " " + player.getLastName()).trim()));
    }

    @Override
    public Optional<PlayersEntity> findByEmail(String email) {
        return playersJpaRepository.findByEmail(email);
    }

    @Override
    public List<PlayersEntity> findAllActive() {
        return playersJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public boolean existsByEmail(String email) {
        return playersJpaRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public void hardDelete(PlayersEntity player) {
        playersJpaRepository.delete(player);
    }

    @Override
    public List<PlayersEntity> findAllActiveByTeamId(UUID teamId) {
        return playersJpaRepository.findAllByTeamIdWithTeam(teamId);
    }
}

