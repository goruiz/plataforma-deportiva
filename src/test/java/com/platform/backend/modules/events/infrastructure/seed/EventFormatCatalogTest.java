package com.platform.backend.modules.events.infrastructure.seed;

import com.platform.backend.modules.events.application.mappers.EventFormatMapper;
import com.platform.backend.modules.events.application.services.EventFormatRulesService;
import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.enums.EventFormatRule;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventFormatCatalogTest {

    private static final int NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final EventFormatMapper mapper = Mappers.getMapper(EventFormatMapper.class);
    private final EventFormatRulesService rulesService = new EventFormatRulesService();

    @Test
    void all_hasTheExpectedNumberOfFormatsForEachStructure() {
        Map<EventFormatStructure, Long> expected = Map.of(
                EventFormatStructure.ROUND_ROBIN, 2L,
                EventFormatStructure.SWISS, 1L,
                EventFormatStructure.KNOCKOUT, 4L,
                EventFormatStructure.DOUBLE_ELIMINATION, 4L,
                EventFormatStructure.GROUPS_KNOCKOUT, 8L,
                EventFormatStructure.LEAGUE_KNOCKOUT, 8L,
                EventFormatStructure.SWISS_KNOCKOUT, 4L);

        for (EventFormatStructure structure : EventFormatStructure.values()) {
            assertThat(countOf(structure)).as(structure.name()).isEqualTo(expected.get(structure));
        }
        assertThat(EventFormatCatalog.all()).hasSize(31);
    }

    @Test
    void all_coversEveryYesNoCombinationOfTheRulesEachStructureUses() {
        for (EventFormatStructure structure : EventFormatStructure.values()) {
            List<EventFormatRule> yesNoRules = structure.rules().stream().filter(EventFormatRule::isYesNo).toList();

            long distinctCombinations = EventFormatCatalog.all().stream()
                    .filter(format -> format.getStructure() == structure)
                    .map(mapper::toEntity)
                    .map(entity -> yesNoRules.stream().map(rule -> String.valueOf(rule.valueIn(entity))).toList())
                    .distinct()
                    .count();

            assertThat(distinctCombinations).as(structure.name()).isEqualTo(1L << yesNoRules.size());
        }
    }

    @Test
    void all_hasNoRepeatedName_ignoringCase() {
        List<String> names = EventFormatCatalog.all().stream()
                .map(format -> format.getName().toLowerCase(Locale.ROOT))
                .toList();

        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    void all_hasNamesAndDescriptionsThatFitTheirColumns() {
        for (CreateEventFormatRequest format : EventFormatCatalog.all()) {
            assertThat(format.getName()).isNotBlank().hasSizeLessThanOrEqualTo(NAME_MAX_LENGTH);
            assertThat(format.getDescription()).isNotBlank().hasSizeLessThanOrEqualTo(DESCRIPTION_MAX_LENGTH);
        }
    }

    @Test
    void all_hasOnlyFormatsThatPassTheRulesValidationUntouched() {
        for (CreateEventFormatRequest format : EventFormatCatalog.all()) {
            EventFormatsEntity entity = mapper.toEntity(format);
            EventFormatResponse beforeValidation = mapper.toResponse(entity);

            rulesService.validateAndNormalize(entity);

            // Nothing had to be cleared: the catalog only sets the rules each structure uses.
            assertThat(mapper.toResponse(entity)).as(format.getName()).isEqualTo(beforeValidation);
        }
    }

    @Test
    void all_keepsTheNamesOfTheFormatsSeededBefore() {
        List<String> names = EventFormatCatalog.all().stream().map(CreateEventFormatRequest::getName).toList();

        assertThat(names).contains(
                "Liga - solo ida",
                "Liga - ida y vuelta",
                "Eliminación directa - partido único",
                "Eliminación directa - ida y vuelta + tercer puesto",
                "Grupos (solo ida) + eliminatoria (partido único)",
                "Grupos (ida y vuelta) + eliminatoria (ida y vuelta) + tercer puesto");
    }

    @Test
    void all_namesTheFormatsOfTheNewerStructures() {
        List<String> names = EventFormatCatalog.all().stream().map(CreateEventFormatRequest::getName).toList();

        assertThat(names).contains(
                "Sistema suizo",
                "Doble eliminación - partido único",
                "Doble eliminación - ida y vuelta + final con revancha",
                "Liga (solo ida) + playoffs (partido único)",
                "Liga (ida y vuelta) + playoffs (ida y vuelta) + tercer puesto",
                "Sistema suizo + playoffs (partido único)");
    }

    private long countOf(EventFormatStructure structure) {
        return EventFormatCatalog.all().stream().filter(format -> format.getStructure() == structure).count();
    }
}
