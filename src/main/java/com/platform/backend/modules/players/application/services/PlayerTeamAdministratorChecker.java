package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.shared.domain.security.TeamAdministratorChecker;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Answers whether an account administers a team from the memberships of its player (the one with the same email). */
@Component
@RequiredArgsConstructor
public class PlayerTeamAdministratorChecker implements TeamAdministratorChecker {

    private final ITeamMemberRepository teamMemberRepository;

    @Override
    public boolean isTeamAdministrator(UUID teamId, String email) {
        return teamMemberRepository.existsActiveTeamAdministrator(teamId, EmailNormalizer.normalize(email));
    }
}
