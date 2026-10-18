package com.platform.backend.modules.events.infrastructure.seed;

import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Every way an event can be played that the format rules can describe: each {@link EventFormatStructure} combined
 * with each yes/no rule it uses (home and away legs, third-place match, grand final rematch). The catalog is
 * generated from those combinations, so it is complete by construction: 2 round-robin, 1 Swiss, 4 knockout,
 * 4 double-elimination, 8 groups + knockout, 8 league + playoffs and 4 Swiss + playoffs formats.
 * <p>
 * The numbers (points, tie-breakers, rounds, groups, playoff teams) are not a dimension of the catalog: each format
 * starts from the usual values below and an admin tunes them, or creates another format, from the panel.
 * <p>
 * The names of the formats already seeded must not change: the seed finds a format by its name, and a renamed one
 * would be created again next to the old one.
 */
final class EventFormatCatalog {

    private static final boolean[] YES_OR_NO = {false, true};

    private static final int POINTS_FOR_WIN = 3;
    private static final int POINTS_FOR_DRAW = 1;
    private static final int POINTS_FOR_LOSS = 0;
    private static final List<TiebreakerCriterion> TIEBREAKERS =
            List.of(TiebreakerCriterion.GOAL_DIFFERENCE, TiebreakerCriterion.GOALS_FOR);
    // Swiss rounds rank teams that never all meet, so they start by the strength of the opponents faced.
    private static final List<TiebreakerCriterion> SWISS_TIEBREAKERS = List.of(
            TiebreakerCriterion.OPPONENTS_POINTS, TiebreakerCriterion.GOAL_DIFFERENCE, TiebreakerCriterion.GOALS_FOR);
    private static final int SWISS_ROUNDS = 5;
    private static final int GROUP_COUNT = 4;
    private static final int TEAMS_ADVANCING_PER_GROUP = 2;
    private static final int PLAYOFF_TEAMS = 4;

    private EventFormatCatalog() {}

    static List<CreateEventFormatRequest> all() {
        List<CreateEventFormatRequest> catalog = new ArrayList<>();

        for (boolean roundTrip : YES_OR_NO) {
            catalog.add(roundRobin(roundTrip));
        }
        catalog.add(swiss());
        for (boolean knockoutRoundTrip : YES_OR_NO) {
            for (boolean thirdPlaceMatch : YES_OR_NO) {
                catalog.add(knockout(knockoutRoundTrip, thirdPlaceMatch));
            }
        }
        for (boolean knockoutRoundTrip : YES_OR_NO) {
            for (boolean grandFinalReset : YES_OR_NO) {
                catalog.add(doubleElimination(knockoutRoundTrip, grandFinalReset));
            }
        }
        for (boolean groupRoundTrip : YES_OR_NO) {
            for (boolean knockoutRoundTrip : YES_OR_NO) {
                for (boolean thirdPlaceMatch : YES_OR_NO) {
                    catalog.add(groupsKnockout(groupRoundTrip, knockoutRoundTrip, thirdPlaceMatch));
                }
            }
        }
        for (boolean leagueRoundTrip : YES_OR_NO) {
            for (boolean knockoutRoundTrip : YES_OR_NO) {
                for (boolean thirdPlaceMatch : YES_OR_NO) {
                    catalog.add(leagueKnockout(leagueRoundTrip, knockoutRoundTrip, thirdPlaceMatch));
                }
            }
        }
        for (boolean knockoutRoundTrip : YES_OR_NO) {
            for (boolean thirdPlaceMatch : YES_OR_NO) {
                catalog.add(swissKnockout(knockoutRoundTrip, thirdPlaceMatch));
            }
        }
        return catalog;
    }

    private static CreateEventFormatRequest roundRobin(boolean roundTrip) {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Liga - " + legs(roundTrip));
        format.setDescription("Todos los equipos se enfrentan " + timesFaced(roundTrip)
                + " y gana quien más puntos suma.");
        format.setStructure(EventFormatStructure.ROUND_ROBIN);
        format.setRoundTrip(roundTrip);
        applyPointsRules(format, TIEBREAKERS);
        return format;
    }

    private static CreateEventFormatRequest swiss() {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Sistema suizo");
        format.setDescription("Los equipos juegan " + SWISS_ROUNDS + " rondas; en cada una se enfrentan equipos con "
                + "puntos parecidos, sin repetir cruces, y gana quien más puntos suma.");
        format.setStructure(EventFormatStructure.SWISS);
        format.setSwissRounds(SWISS_ROUNDS);
        applyPointsRules(format, SWISS_TIEBREAKERS);
        return format;
    }

    private static CreateEventFormatRequest knockout(boolean knockoutRoundTrip, boolean thirdPlaceMatch) {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Eliminación directa - " + knockoutLegs(knockoutRoundTrip) + thirdPlaceNameSuffix(thirdPlaceMatch));
        format.setDescription("Quien pierde queda eliminado y los ganadores avanzan de ronda hasta la final. "
                + "Cada cruce se juega a " + knockoutLegs(knockoutRoundTrip) + thirdPlaceDescriptionSuffix(thirdPlaceMatch) + ".");
        format.setStructure(EventFormatStructure.KNOCKOUT);
        format.setKnockoutRoundTrip(knockoutRoundTrip);
        format.setThirdPlaceMatch(thirdPlaceMatch);
        return format;
    }

    private static CreateEventFormatRequest doubleElimination(boolean knockoutRoundTrip, boolean grandFinalReset) {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Doble eliminación - " + knockoutLegs(knockoutRoundTrip)
                + (grandFinalReset ? " + final con revancha" : ""));
        format.setDescription("Un equipo queda eliminado al perder dos veces: quien pierde la primera pasa a la llave de "
                + "perdedores. Cada cruce se juega a " + knockoutLegs(knockoutRoundTrip)
                + (grandFinalReset
                ? "; si gana la final quien llegó por la llave de perdedores, se juega una revancha."
                : "; la final se juega una sola vez."));
        format.setStructure(EventFormatStructure.DOUBLE_ELIMINATION);
        format.setKnockoutRoundTrip(knockoutRoundTrip);
        format.setGrandFinalReset(grandFinalReset);
        return format;
    }

    private static CreateEventFormatRequest groupsKnockout(boolean groupRoundTrip, boolean knockoutRoundTrip, boolean thirdPlaceMatch) {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Grupos (" + legs(groupRoundTrip) + ") + eliminatoria (" + knockoutLegs(knockoutRoundTrip) + ")"
                + thirdPlaceNameSuffix(thirdPlaceMatch));
        format.setDescription("Fase de grupos (" + GROUP_COUNT + " grupos, clasifican " + TEAMS_ADVANCING_PER_GROUP
                + " por grupo) donde cada pareja de equipos se enfrenta " + timesFaced(groupRoundTrip)
                + "; luego eliminatoria a " + knockoutLegs(knockoutRoundTrip) + thirdPlaceDescriptionSuffix(thirdPlaceMatch) + ".");
        format.setStructure(EventFormatStructure.GROUPS_KNOCKOUT);
        format.setRoundTrip(groupRoundTrip);
        applyPointsRules(format, TIEBREAKERS);
        format.setGroupCount(GROUP_COUNT);
        format.setTeamsAdvancingPerGroup(TEAMS_ADVANCING_PER_GROUP);
        format.setKnockoutRoundTrip(knockoutRoundTrip);
        format.setThirdPlaceMatch(thirdPlaceMatch);
        return format;
    }

    private static CreateEventFormatRequest leagueKnockout(boolean leagueRoundTrip, boolean knockoutRoundTrip, boolean thirdPlaceMatch) {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Liga (" + legs(leagueRoundTrip) + ") + playoffs (" + knockoutLegs(knockoutRoundTrip) + ")"
                + thirdPlaceNameSuffix(thirdPlaceMatch));
        format.setDescription("Todos los equipos se enfrentan " + timesFaced(leagueRoundTrip) + " y los " + PLAYOFF_TEAMS
                + " mejores pasan a playoffs, que se juegan a " + knockoutLegs(knockoutRoundTrip)
                + thirdPlaceDescriptionSuffix(thirdPlaceMatch) + ".");
        format.setStructure(EventFormatStructure.LEAGUE_KNOCKOUT);
        format.setRoundTrip(leagueRoundTrip);
        applyPointsRules(format, TIEBREAKERS);
        format.setPlayoffTeams(PLAYOFF_TEAMS);
        format.setKnockoutRoundTrip(knockoutRoundTrip);
        format.setThirdPlaceMatch(thirdPlaceMatch);
        return format;
    }

    private static CreateEventFormatRequest swissKnockout(boolean knockoutRoundTrip, boolean thirdPlaceMatch) {
        CreateEventFormatRequest format = new CreateEventFormatRequest();
        format.setName("Sistema suizo + playoffs (" + knockoutLegs(knockoutRoundTrip) + ")" + thirdPlaceNameSuffix(thirdPlaceMatch));
        format.setDescription("Los equipos juegan " + SWISS_ROUNDS + " rondas suizas y los " + PLAYOFF_TEAMS
                + " mejores pasan a playoffs, que se juegan a " + knockoutLegs(knockoutRoundTrip)
                + thirdPlaceDescriptionSuffix(thirdPlaceMatch) + ".");
        format.setStructure(EventFormatStructure.SWISS_KNOCKOUT);
        format.setSwissRounds(SWISS_ROUNDS);
        applyPointsRules(format, SWISS_TIEBREAKERS);
        format.setPlayoffTeams(PLAYOFF_TEAMS);
        format.setKnockoutRoundTrip(knockoutRoundTrip);
        format.setThirdPlaceMatch(thirdPlaceMatch);
        return format;
    }

    private static void applyPointsRules(CreateEventFormatRequest format, List<TiebreakerCriterion> tiebreakers) {
        format.setPointsForWin(POINTS_FOR_WIN);
        format.setPointsForDraw(POINTS_FOR_DRAW);
        format.setPointsForLoss(POINTS_FOR_LOSS);
        format.setTiebreakers(tiebreakers);
    }

    private static String legs(boolean roundTrip) {
        return roundTrip ? "ida y vuelta" : "solo ida";
    }

    private static String knockoutLegs(boolean roundTrip) {
        return roundTrip ? "ida y vuelta" : "partido único";
    }

    private static String timesFaced(boolean roundTrip) {
        return roundTrip ? "dos veces, una como local y otra como visitante" : "una sola vez";
    }

    private static String thirdPlaceNameSuffix(boolean thirdPlaceMatch) {
        return thirdPlaceMatch ? " + tercer puesto" : "";
    }

    private static String thirdPlaceDescriptionSuffix(boolean thirdPlaceMatch) {
        return thirdPlaceMatch ? ", con partido por el tercer puesto" : "";
    }
}
