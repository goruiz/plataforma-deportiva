package com.platform.backend.modules.players.infrastructure.persistence.repositories;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;
import com.platform.backend.modules.players.domain.irepositories.ITeamInvitationRepository;
import com.platform.backend.modules.players.infrastructure.persistence.jpa.TeamInvitationsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TeamInvitationRepository implements ITeamInvitationRepository {

    private final TeamInvitationsJpaRepository jpaRepository;

    @Override
    public TeamInvitationsEntity save(TeamInvitationsEntity invitation) {
        return jpaRepository.save(invitation);
    }

    @Override
    public Optional<TeamInvitationsEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<TeamInvitationsEntity> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }
}
