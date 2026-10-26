package com.platform.backend.modules.players.domain.irepositories;

import com.platform.backend.modules.players.domain.entities.TeamInvitationsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITeamInvitationRepository {

    TeamInvitationsEntity save(TeamInvitationsEntity invitation);

    Optional<TeamInvitationsEntity> findById(UUID id);

    /** The invitations addressed to an email (ignoring case) that are in the given status, newest first. */
    List<TeamInvitationsEntity> findAllActiveByEmailAndStatus(String email, String status);

    Optional<TeamInvitationsEntity> findByToken(String token);
}
