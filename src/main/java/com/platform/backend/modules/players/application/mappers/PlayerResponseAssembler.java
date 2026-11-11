package com.platform.backend.modules.players.application.mappers;

import com.platform.backend.modules.events.domain.models.CategoryRequirements;
import com.platform.backend.modules.players.application.services.PlayerEligibilityPolicy;
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
 * <p>
 * Each team also says whether the player meets what its category asks (gender, age). The rules of the categories are read
 * once for all the teams of a response (or of a whole list), never once per player.
 */
@Component
@RequiredArgsConstructor
public class PlayerResponseAssembler {

    private final PlayerMapper playerMapper;
    private final ITeamMemberRepository teamMemberRepository;
    private final PlayerEligibilityPolicy eligibilityPolicy;

    /** The player with every team the caller can see them in. */
    public PlayerResponse toResponse(PlayersEntity player) {
        return toResponse(player, teamMemberRepository.findAllActiveByPlayerId(player.getId()));
    }

    /** The players with their teams, read together (one query for the whole list, not one per player). */
    public List<PlayerResponse> toResponses(Collection<PlayersEntity> players) {
        return toResponses(players, membershipsByPlayerId(players));
    }

    /** The players with memberships the caller already read (see {@link #membershipsByPlayerId}). */
    public List<PlayerResponse> toResponses(Collection<PlayersEntity> players,
                                            Map<UUID, List<TeamMembersEntity>> membershipsByPlayer) {
        List<TeamMembersEntity> memberships = players.stream()
                .flatMap(player -> membershipsByPlayer.getOrDefault(player.getId(), List.of()).stream())
                .toList();
        Map<UUID, CategoryRequirements> requirements = requirementsOfTeamsIn(memberships);
        return players.stream()
                .map(player -> toResponse(player, membershipsByPlayer.getOrDefault(player.getId(), List.of()), requirements))
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
        return toResponse(player, memberships, requirementsOfTeamsIn(memberships));
    }

    private PlayerResponse toResponse(PlayersEntity player, List<TeamMembersEntity> memberships,
                                      Map<UUID, CategoryRequirements> requirementsByCategory) {
        PlayerResponse response = playerMapper.toResponse(player);
        response.setTeams(memberships.stream()
                .map(membership -> toTeamResponse(player, membership, requirementsByCategory))
                .toList());
        memberships.stream().findFirst().ifPresent(first -> {
            response.setTeamId(first.getTeam().getId());
            response.setTeamName(first.getTeam().getName());
        });
        response.setIsCaptain(memberships.stream().findFirst().map(TeamMembersEntity::isCaptainOfTheTeam).orElse(false));
        response.setIsTeamAdmin(memberships.stream().findFirst().map(TeamMembersEntity::isAdministratorOfTheTeam).orElse(false));
        return response;
    }

    private Map<UUID, CategoryRequirements> requirementsOfTeamsIn(List<TeamMembersEntity> memberships) {
        return eligibilityPolicy.requirementsOfCategoriesOf(memberships.stream().map(TeamMembersEntity::getTeam).toList());
    }

    private PlayerTeamResponse toTeamResponse(PlayersEntity player, TeamMembersEntity membership,
                                              Map<UUID, CategoryRequirements> requirementsByCategory) {
        PlayerTeamResponse response = new PlayerTeamResponse(
                membership.getTeam().getId(),
                membership.getTeam().getName(),
                membership.getOrganizationId(),
                membership.isCaptainOfTheTeam(),
                membership.isAdministratorOfTheTeam());
        CategoryRequirements requirements = PlayerEligibilityPolicy
                .requirementFor(requirementsByCategory, membership.getTeam().getCategoryId());
        eligibilityPolicy.reasonWhyNot(requirements, player.getGender(), player.getBirthDate()).ifPresent(reason -> {
            response.setEligibleForCategory(false);
            response.setCategoryIneligibility(reason.name());
        });
        return response;
    }
}
