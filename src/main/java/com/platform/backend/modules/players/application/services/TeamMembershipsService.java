package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.application.iservices.ITeamMembershipsService;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamMembershipsService implements ITeamMembershipsService {

    private final ITeamMemberRepository teamMemberRepository;
    private final IPlayerAccountOrganizationService playerAccountOrganizationService;

    @Override
    @Transactional
    public TeamMembersEntity join(PlayersEntity player, TeamsEntity team) {
        List<TeamMembersEntity> current = teamMemberRepository.findAllActiveByPlayerId(player.getId());

        TeamMembersEntity alreadyThere = current.stream()
                .filter(membership -> membership.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElse(null);
        if (alreadyThere != null) {
            return alreadyThere;
        }

        current.stream()
                .filter(membership -> sameOrganization(membership.getOrganizationId(), team.getOrganizationId()))
                .forEach(this::endNow);

        TeamMembersEntity membership = teamMemberRepository.save(TeamMembersEntity.of(player, team));
        playerAccountOrganizationService.followTeam(player.getEmail(), team.getOrganizationId());
        return membership;
    }

    @Override
    @Transactional
    public void leave(TeamMembersEntity membership) {
        endNow(membership);
        followTheTeamJoinedLast(membership.getPlayer());
    }

    @Override
    @Transactional
    public void leaveEvery(PlayersEntity player) {
        teamMemberRepository.findAllActiveByPlayerId(player.getId()).forEach(this::endNow);
        followTheTeamJoinedLast(player);
    }

    /**
     * Written at once: a unique index allows one team per organization, and Hibernate runs the inserts of a transaction
     * before its updates, so the new team would meet the old one if both waited for the end.
     */
    private void endNow(TeamMembersEntity membership) {
        membership.end();
        teamMemberRepository.saveAndFlush(membership);
    }

    /**
     * The account keeps a single organization (the one of the last team joined): what a player sees comes from all their
     * teams, but the list of an organization's app users and the token still read this one.
     */
    private void followTheTeamJoinedLast(PlayersEntity player) {
        // Read without the organization filter: whoever removes the player from a team does not see their other teams.
        UUID organizationId = teamMemberRepository.findOrganizationIdOfLastJoinedTeam(player.getId()).orElse(null);
        playerAccountOrganizationService.followTeam(player.getEmail(), organizationId);
    }

    /** A team without organization is the platform owner's: all of those count as one organization. */
    private static boolean sameOrganization(UUID first, UUID second) {
        return Objects.equals(first, second);
    }
}
