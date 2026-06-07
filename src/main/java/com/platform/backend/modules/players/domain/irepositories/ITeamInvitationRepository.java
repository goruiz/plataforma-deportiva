package com.platform.backend.modules.players.domain.irepositories;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;

import java.util.Optional;
import java.util.UUID;

public interface ITeamInvitationRepository {

    TeamInvitationsEntity save(TeamInvitationsEntity invitation);

    Optional<TeamInvitationsEntity> findById(UUID id);

    Optional<TeamInvitationsEntity> findByToken(String token);
}
