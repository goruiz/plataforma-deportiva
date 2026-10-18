package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventFormatRulesService;
import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.enums.EventFormatRule;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.events.domain.exception.EventFormatError;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventFormatRulesService implements IEventFormatRulesService {

    private static final String STRUCTURE_FIELD = "structure";

    @Override
    public void validateAndNormalize(EventFormatsEntity format) {
        EventFormatStructure structure = format.getStructure();
        if (structure == null) {
            rejectRulesWithoutStructure(format);
            return;
        }

        Map<String, String> rejectedFields = new LinkedHashMap<>();
        collectMissingRules(format, structure, rejectedFields);
        collectInconsistentRules(format, structure, rejectedFields);
        if (!rejectedFields.isEmpty()) {
            throw new FieldValidationException(rejectedFields);
        }

        clearRulesTheStructureDoesNotUse(format, structure);
    }

    private void rejectRulesWithoutStructure(EventFormatsEntity format) {
        boolean hasAnyRule = Arrays.stream(EventFormatRule.values()).anyMatch(rule -> rule.valueIn(format) != null);
        if (hasAnyRule) {
            throw new FieldValidationException(STRUCTURE_FIELD, EventFormatError.RULE_REQUIRED.name());
        }
    }

    private void collectMissingRules(EventFormatsEntity format, EventFormatStructure structure, Map<String, String> rejectedFields) {
        for (EventFormatRule rule : structure.rules()) {
            if (rule.valueIn(format) == null) {
                rejectedFields.put(rule.fieldName(), EventFormatError.RULE_REQUIRED.name());
            }
        }
    }

    /** Only looks at rules that are present: a missing one is already reported by {@link #collectMissingRules}. */
    private void collectInconsistentRules(EventFormatsEntity format, EventFormatStructure structure, Map<String, String> rejectedFields) {
        if (structure.uses(EventFormatRule.POINTS_FOR_WIN)
                && arePointsOutOfOrder(format.getPointsForWin(), format.getPointsForDraw(), format.getPointsForLoss())) {
            rejectedFields.put(EventFormatRule.POINTS_FOR_WIN.fieldName(), EventFormatError.POINTS_ORDER_INVALID.name());
        }
        if (structure.uses(EventFormatRule.TIEBREAKERS) && hasRepeatedCriterion(TiebreakerCriterion.fromStored(format.getTiebreakers()))) {
            rejectedFields.put(EventFormatRule.TIEBREAKERS.fieldName(), EventFormatError.TIEBREAKERS_DUPLICATED.name());
        }
    }

    private boolean arePointsOutOfOrder(Integer win, Integer draw, Integer loss) {
        if (win == null || draw == null || loss == null) {
            return false;
        }
        return win < draw || draw < loss || win <= loss;
    }

    private boolean hasRepeatedCriterion(List<TiebreakerCriterion> criteria) {
        return criteria != null && new HashSet<>(criteria).size() != criteria.size();
    }

    private void clearRulesTheStructureDoesNotUse(EventFormatsEntity format, EventFormatStructure structure) {
        Arrays.stream(EventFormatRule.values())
                .filter(rule -> !structure.uses(rule))
                .forEach(rule -> rule.clearIn(format));
    }
}
