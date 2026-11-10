package com.platform.backend.modules.players.application.mappers;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;
import com.platform.backend.modules.players.presentation.responses.PlayerTeamResponse.PlayerTeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the response of a player: the person from {@link PlayerMapper} and the teams they play for from their memberships.
 * <p>
 * The memberships it reads go through the organization filter, so what a response says about a player's teams is what the
 * caller may see: an administrator only the team of their organization, the player themselves all of theirs.
 */
@Component
@RequiredArgsConstructor
public class PlayerResponseAssembler {

    private final PlayerMapper playerMapper;
    private final ITeamMemberRepository teamMemberRepository;

    /** The player with every team the caller can see them in. */
    public PlayerResponse toResponse(PlayersEntity player) {
        return toResponse(player, teamMemberRepository.findAllActiveByPlayerId(player.getId()));
    }

    /** The players with their teams, read together (one query for the whole list, not one per player). */
    public List<PlayerResponse> toResponses(Collection<PlayersEntity> players) {
        Map<UUID, List<TeamMembersEntity>> membershipsByPlayer = membershipsByPlayerId(players);
        return players.stream()
                .map(player -> toResponse(player, membershipsByPlayer.getOrDefault(player.getId(), List.of())))
                .toList();
    }

    /** The memberships of each player the caller can see, the last one joined first. */
    public Map<UUID, List<TeamMembersEntity>> membershipsByPlayerId(Collection<PlayersEntity> players) {
        List<UUID> playerIds = players.stream().map(PlayersEntity::getId).toList();
        return teamMemberRepository.findAllActiveByPlayerIds(playerIds).stream()
                .collect(Collectors.groupingBy(membership -> membership.getPlayer().getId()));
    }

    /**
     * The player seen through the given memberships (the most recent first). {@code teamId}, {@code teamName} and
     * {@code isCaptain} describe the first one, which is what clients that know a single team read.
     */
    public PlayerResponse toResponse(PlayersEntity player, List<TeamMembersEntity> memberships) {
        PlayerResponse response = playerMapper.toResponse(player);
        response.setTeams(memberships.stream().map(PlayerResponseAssembler::toTeamResponse).toList());
        memberships.stream().findFirst().ifPresent(first -> {
            response.setTeamId(first.getTeam().getId());
            response.setTeamName(first.getTeam().getName());
        });
        response.setIsCaptain(memberships.stream().findFirst().map(TeamMembersEntity::isCaptainOfTheTeam).orElse(false));
        response.setIsTeamAdmin(memberships.stream().findFirst().map(TeamMembersEntity::isAdministratorOfTheTeam).orElse(false));
        return response;
    }

    private static PlayerTeamResponse toTeamResponse(TeamMembersEntity membership) {
        return new PlayerTeamResponse(
                membership.getTeam().getId(),
                membership.getTeam().getName(),
                membership.getOrganizationId(),
                membership.isCaptainOfTheTeam(),
                membership.isAdministratorOfTheTeam());
    }
}
