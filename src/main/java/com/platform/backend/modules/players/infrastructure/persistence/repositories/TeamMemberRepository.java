package com.platform.backend.modules.players.infrastructure.persistence.repositories;

import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.players.infrastructure.persistence.jpa.TeamMembersJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TeamMemberRepository implements ITeamMemberRepository {

    private final TeamMembersJpaRepository jpaRepository;

    @Override
    public TeamMembersEntity save(TeamMembersEntity membership) {
        return jpaRepository.save(membership);
    }

    @Override
    public TeamMembersEntity saveAndFlush(TeamMembersEntity membership) {
        return jpaRepository.saveAndFlush(membership);
    }

    @Override
    public List<TeamMembersEntity> findAllActiveByPlayerId(UUID playerId) {
        return jpaRepository.findAllActiveByPlayerId(playerId);
    }

    @Override
    public List<TeamMembersEntity> findAllActiveByPlayerIds(Collection<UUID> playerIds) {
        return playerIds.isEmpty() ? List.of() : jpaRepository.findAllActiveByPlayerIds(playerIds);
    }

    @Override
    public List<TeamMembersEntity> findAllActiveByTeamId(UUID teamId) {
        return jpaRepository.findAllActiveByTeamId(teamId);
    }

    @Override
    public Optional<TeamMembersEntity> findActiveByPlayerIdAndTeamId(UUID playerId, UUID teamId) {
        return jpaRepository.findActiveByPlayerIdAndTeamId(playerId, teamId);
    }

    @Override
    public boolean existsActiveByPlayerIdAndTeamId(UUID playerId, UUID teamId) {
        return jpaRepository.existsActiveByPlayerIdAndTeamId(playerId, teamId);
    }

    @Override
    public Optional<UUID> findOrganizationIdOfLastJoinedTeam(UUID playerId) {
        return jpaRepository.findOrganizationIdOfLastJoinedTeam(playerId);
    }

    @Override
    public boolean existsActiveOutsideOrganization(UUID playerId, UUID organizationId) {
        return jpaRepository.existsActiveOutsideOrganization(playerId, organizationId);
    }
}
