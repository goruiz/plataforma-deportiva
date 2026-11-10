package com.platform.backend.modules.matches.application.mappers;

import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.MatchDisputeResponse.MatchDisputeResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dresses disputes with the match, team and event names a screen needs. One query per kind for the whole list, not one
 * per dispute; whatever the caller does not see simply has no name.
 */
@Component
@RequiredArgsConstructor
public class MatchDisputeResponseAssembler {

    private final IMatchRepository matchRepository;
    private final ITeamRepository teamRepository;
    private final IEventRepository eventRepository;

    public List<MatchDisputeResponse> toResponses(List<MatchDisputesEntity> disputes) {
        Map<UUID, MatchesEntity> matches = readMatches(disputes);
        Map<UUID, String> teamNames = readTeamNames(disputes, matches);
        Map<UUID, String> eventNames = readEventNames(matches);
        return disputes.stream()
                .map(dispute -> toResponse(dispute, matches.get(dispute.getMatchId()), teamNames, eventNames))
                .toList();
    }

    private MatchDisputeResponse toResponse(MatchDisputesEntity dispute, MatchesEntity match,
                                            Map<UUID, String> teamNames, Map<UUID, String> eventNames) {
        MatchDisputeResponse response = new MatchDisputeResponse();
        response.setId(dispute.getId());
        response.setMatchId(dispute.getMatchId());
        response.setStatus(dispute.getStatus());
        response.setTeamId(dispute.getTeamId());
        response.setTeamName(teamNames.get(dispute.getTeamId()));
        response.setReason(dispute.getReason());
        response.setProposedHomeScore(dispute.getProposedHomeScore());
        response.setProposedAwayScore(dispute.getProposedAwayScore());
        response.setOpenedById(dispute.getCreatedBy());
        response.setOpenedByName(dispute.getOpenedByName());
        response.setCreatedAt(dispute.getCreatedAt());
        response.setResolutionNote(dispute.getResolutionNote());
        response.setResolvedByName(dispute.getResolvedByName());
        response.setResolvedAt(dispute.getResolvedAt());
        if (match != null) {
            describeMatch(response, match, teamNames, eventNames);
        }
        return response;
    }

    private void describeMatch(MatchDisputeResponse response, MatchesEntity match,
                               Map<UUID, String> teamNames, Map<UUID, String> eventNames) {
        response.setEventId(match.getEventId());
        response.setEventName(eventNames.get(match.getEventId()));
        response.setHomeTeamId(match.getHomeTeamId());
        response.setAwayTeamId(match.getAwayTeamId());
        response.setHomeTeamName(teamNames.get(match.getHomeTeamId()));
        response.setAwayTeamName(teamNames.get(match.getAwayTeamId()));
        response.setMatchDate(match.getMatchDate());
        response.setHomeScore(match.getHomeScore());
        response.setAwayScore(match.getAwayScore());
        response.setHomePenaltyScore(match.getHomePenaltyScore());
        response.setAwayPenaltyScore(match.getAwayPenaltyScore());
    }

    private Map<UUID, MatchesEntity> readMatches(List<MatchDisputesEntity> disputes) {
        Set<UUID> matchIds = disputes.stream().map(MatchDisputesEntity::getMatchId).collect(Collectors.toSet());
        if (matchIds.isEmpty()) {
            return Map.of();
        }
        return matchRepository.findAllActiveByIds(matchIds).stream()
                .collect(Collectors.toMap(MatchesEntity::getId, Function.identity()));
    }

    private Map<UUID, String> readTeamNames(List<MatchDisputesEntity> disputes, Map<UUID, MatchesEntity> matches) {
        Set<UUID> teamIds = new HashSet<>();
        disputes.forEach(dispute -> teamIds.add(dispute.getTeamId()));
        matches.values().forEach(match -> {
            teamIds.add(match.getHomeTeamId());
            teamIds.add(match.getAwayTeamId());
        });
        teamIds.remove(null);
        Map<UUID, String> names = new HashMap<>();
        if (!teamIds.isEmpty()) {
            teamRepository.findAllActiveByIds(teamIds).forEach(team -> names.put(team.getId(), team.getName()));
        }
        return names;
    }

    private Map<UUID, String> readEventNames(Map<UUID, MatchesEntity> matches) {
        Set<UUID> eventIds = matches.values().stream()
                .map(MatchesEntity::getEventId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = new HashMap<>();
        if (!eventIds.isEmpty()) {
            eventRepository.findAllActiveByIds(eventIds).forEach(event -> names.put(event.getId(), event.getName()));
        }
        return names;
    }
}
