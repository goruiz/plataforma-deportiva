package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.engine.ChampionshipPlanner;
import com.platform.backend.modules.matches.application.engine.ChampionshipProgress;
import com.platform.backend.modules.matches.application.engine.ChampionshipRules;
import com.platform.backend.modules.matches.application.engine.GroupDraw;
import com.platform.backend.modules.matches.application.engine.MatchRef;
import com.platform.backend.modules.matches.application.engine.TeamRef;
import com.platform.backend.modules.matches.application.engine.TieResolver;
import com.platform.backend.modules.matches.application.iservices.IChampionshipService;
import com.platform.backend.modules.matches.application.iservices.IEventScheduleConfigService;
import com.platform.backend.modules.matches.application.mappers.ChampionshipResponseMapper;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.exception.ChampionshipError;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.ChampionshipResponse.ChampionshipResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Service
@RequiredArgsConstructor
public class ChampionshipService implements IChampionshipService {

    /** The order of every draw (groups, bracket of a knockout without a previous phase): random. */
    private static final UnaryOperator<List<UUID>> RANDOM_DRAW = teams -> {
        List<UUID> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);
        return shuffled;
    };

    private static final Comparator<TeamsEventsEntity> BY_REGISTRATION_ORDER =
            Comparator.comparing(TeamsEventsEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(registration -> registration.getTeam().getId());

    private final IEventRepository eventRepository;
    private final ITeamsEventsRepository teamsEventsRepository;
    private final IMatchRepository matchRepository;
    private final IEventScheduleConfigRepository scheduleConfigRepository;
    private final IEventScheduleConfigService scheduleService;
    private final ChampionshipResponseMapper responseMapper;

    @Override
    @Transactional(readOnly = true)
    public ChampionshipResponse getChampionship(UUID eventId) {
        return buildResponse(findEvent(eventId));
    }

    @Override
    @Transactional
    public ChampionshipResponse drawGroups(UUID eventId) {
        EventsEntity event = findEvent(eventId);
        ChampionshipRules rules = requireRules(event);
        if (!rules.structure().phases().contains(ChampionshipPhase.GROUPS)) {
            throw new IllegalStateException(ChampionshipError.FORMAT_HAS_NO_GROUPS.name());
        }
        List<TeamsEventsEntity> registrations = registrationsOf(eventId);
        ChampionshipProgress progress = progressOf(rules, registrations, matchRefsOf(eventId));
        if (!progress.matchesOf(ChampionshipPhase.GROUPS).isEmpty()) {
            throw new IllegalStateException(ChampionshipError.GROUPS_ALREADY_GENERATED.name());
        }
        if (registrations.size() < ChampionshipProgress.requiredTeams(rules)) {
            throw new IllegalStateException(ChampionshipAction.NOT_ENOUGH_TEAMS.name());
        }
        assignGroups(rules, registrations);
        return buildResponse(event);
    }

    @Override
    @Transactional
    public ChampionshipResponse generateNext(UUID eventId) {
        EventsEntity event = findEvent(eventId);
        ChampionshipRules rules = requireRules(event);
        if (scheduleConfigRepository.findActiveByEventId(eventId).isEmpty()) {
            throw new IllegalStateException(ChampionshipError.SCHEDULE_CONFIG_REQUIRED.name());
        }

        List<TeamsEventsEntity> registrations = registrationsOf(eventId);
        List<MatchRef> matches = matchRefsOf(eventId);
        if (mustDrawGroupsFirst(progressOf(rules, registrations, matches))) {
            assignGroups(rules, registrations);
        }

        ChampionshipPlanner.Plan plan = ChampionshipPlanner.planNext(rules, teamRefsOf(registrations), matches, RANDOM_DRAW);
        saveSeeds(registrations, plan.seeds());
        scheduleService.scheduleDrafts(eventId, plan.drafts());
        return buildResponse(event);
    }

    @Override
    @Transactional
    public ChampionshipResponse setAdvancingTeam(UUID eventId, UUID matchId, UUID teamId) {
        EventsEntity event = findEvent(eventId);
        ChampionshipRules rules = requireRules(event);
        MatchesEntity match = matchRepository.findActiveById(matchId)
                .filter(found -> eventId.equals(found.getEventId()))
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));

        ChampionshipProgress progress = progressOf(rules, registrationsOf(eventId), matchRefsOf(eventId));
        validateTieNeedsTiebreak(match, teamId, progress);
        match.setAdvancingTeamId(teamId);
        matchRepository.save(match);
        return buildResponse(event);
    }

    // ------------------------------------------------------------------ rules of the tie-break

    private void validateTieNeedsTiebreak(MatchesEntity match, UUID teamId, ChampionshipProgress progress) {
        MatchStage stage = match.getStage();
        if (stage == null || !stage.phase().isElimination()) {
            throw new IllegalStateException(ChampionshipError.NOT_AN_ELIMINATION_MATCH.name());
        }
        ChampionshipProgress.TieView tie = progress.rounds().stream()
                .flatMap(round -> round.ties().stream())
                .filter(candidate -> candidate.legs().stream().anyMatch(leg -> leg.id().equals(match.getId())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ChampionshipError.NOT_AN_ELIMINATION_MATCH.name()));

        if (!TieResolver.decidingLeg(tie.legs()).id().equals(match.getId())) {
            throw new IllegalStateException(ChampionshipError.NOT_THE_DECIDING_MATCH.name());
        }
        boolean endedLevel = tie.legs().stream().allMatch(MatchRef::isDecided) && tie.teamAGoals() == tie.teamBGoals();
        if (!endedLevel) {
            throw new IllegalStateException(ChampionshipError.TIE_NOT_LEVEL.name());
        }
        if (!teamId.equals(tie.teamAId()) && !teamId.equals(tie.teamBId())) {
            throw new IllegalStateException(ChampionshipError.TEAM_NOT_IN_TIE.name());
        }
    }

    // ------------------------------------------------------------------ groups and seeds

    /** The groups are drawn together with the group stage calendar the first time, so nobody has to ask for the draw. */
    private boolean mustDrawGroupsFirst(ChampionshipProgress progress) {
        ChampionshipProgress.NextStep step = progress.nextStep();
        return step.action() == ChampionshipAction.GENERATE_CALENDAR
                && step.phase() == ChampionshipPhase.GROUPS
                && !progress.areGroupsDrawn();
    }

    private void assignGroups(ChampionshipRules rules, List<TeamsEventsEntity> registrations) {
        List<UUID> teamIds = registrations.stream().map(registration -> registration.getTeam().getId()).toList();
        Map<UUID, Integer> groupOfTeam = GroupDraw.assign(teamIds, rules.groupCount(), RANDOM_DRAW);
        for (TeamsEventsEntity registration : registrations) {
            registration.setGroupNumber(groupOfTeam.get(registration.getTeam().getId()));
            teamsEventsRepository.save(registration);
        }
    }

    /** When an elimination starts every team gets its place (or none, if it did not qualify). */
    private void saveSeeds(List<TeamsEventsEntity> registrations, Map<UUID, Integer> seeds) {
        if (seeds.isEmpty()) {
            return;
        }
        for (TeamsEventsEntity registration : registrations) {
            registration.setSeed(seeds.get(registration.getTeam().getId()));
            teamsEventsRepository.save(registration);
        }
    }

    // ------------------------------------------------------------------ reading

    private ChampionshipResponse buildResponse(EventsEntity event) {
        List<TeamsEventsEntity> registrations = registrationsOf(event.getId());
        List<TeamRef> teams = teamRefsOf(registrations);
        Optional<ChampionshipRules> rules = rulesOf(event);
        if (rules.isEmpty()) {
            return responseMapper.toResponseWithoutChampionship(event, teams);
        }
        boolean scheduleConfigured = scheduleConfigRepository.findActiveByEventId(event.getId()).isPresent();
        ChampionshipProgress progress = ChampionshipProgress.of(rules.get(), teams, matchRefsOf(event.getId()));
        return responseMapper.toResponse(event, rules.get(), progress, teams, scheduleConfigured);
    }

    private ChampionshipProgress progressOf(ChampionshipRules rules, List<TeamsEventsEntity> registrations, List<MatchRef> matches) {
        return ChampionshipProgress.of(rules, teamRefsOf(registrations), matches);
    }

    private EventsEntity findEvent(UUID eventId) {
        return eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
    }

    /** A format whose rules were never defined (created before rules existed) cannot run a championship. */
    private Optional<ChampionshipRules> rulesOf(EventsEntity event) {
        EventFormatsEntity format = event.getEventFormat();
        if (format == null || format.getStructure() == null) {
            return Optional.empty();
        }
        return Optional.of(ChampionshipRules.of(format));
    }

    private ChampionshipRules requireRules(EventsEntity event) {
        return rulesOf(event).orElseThrow(() -> new IllegalStateException(ChampionshipAction.NO_FORMAT.name()));
    }

    private List<TeamsEventsEntity> registrationsOf(UUID eventId) {
        return teamsEventsRepository.findAllActiveByEventId(eventId).stream().sorted(BY_REGISTRATION_ORDER).toList();
    }

    private List<TeamRef> teamRefsOf(List<TeamsEventsEntity> registrations) {
        return registrations.stream()
                .map(registration -> new TeamRef(registration.getTeam().getId(), registration.getTeam().getName(),
                        registration.getGroupNumber(), registration.getSeed()))
                .toList();
    }

    private List<MatchRef> matchRefsOf(UUID eventId) {
        return matchRepository.findAllActiveByEventId(eventId).stream().map(ChampionshipService::toMatchRef).toList();
    }

    private static MatchRef toMatchRef(MatchesEntity match) {
        MatchStage stage = match.getStage() == null ? MatchStage.LEAGUE : match.getStage();
        return new MatchRef(match.getId(), match.getHomeTeamId(), match.getAwayTeamId(), match.getHomeScore(), match.getAwayScore(),
                stage, match.getRoundNumber(), match.getGroupNumber(), match.getTieNumber(), match.getLegNumber(),
                match.getAdvancingTeamId(), match.getMatchDate(), match.getStatus());
    }
}
