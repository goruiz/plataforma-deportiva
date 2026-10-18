package com.platform.backend.modules.events.application.mappers;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventFormatMapperTest {

    private final EventFormatMapper mapper = Mappers.getMapper(EventFormatMapper.class);

    @Test
    void toEntity_whenRequestHasRules_thenKeepTiebreakersInPriorityOrder() {
        CreateEventFormatRequest request = new CreateEventFormatRequest();
        request.setName("Liga");
        request.setStructure(EventFormatStructure.ROUND_ROBIN);
        request.setPointsForWin(3);
        request.setTiebreakers(List.of(TiebreakerCriterion.GOALS_FOR, TiebreakerCriterion.GOAL_DIFFERENCE));

        EventFormatsEntity entity = mapper.toEntity(request);

        assertThat(entity.getStructure()).isEqualTo(EventFormatStructure.ROUND_ROBIN);
        assertThat(entity.getPointsForWin()).isEqualTo(3);
        assertThat(entity.getTiebreakers()).isEqualTo("GOALS_FOR,GOAL_DIFFERENCE");
    }

    @Test
    void toEntityAndBack_whenFormatHasSwissAndPlayoffRules_thenKeepThem() {
        CreateEventFormatRequest request = new CreateEventFormatRequest();
        request.setName("Suizo");
        request.setStructure(EventFormatStructure.SWISS_KNOCKOUT);
        request.setSwissRounds(5);
        request.setPlayoffTeams(8);
        request.setGrandFinalReset(true);

        EventFormatResponse response = mapper.toResponse(mapper.toEntity(request));

        assertThat(response.getStructure()).isEqualTo(EventFormatStructure.SWISS_KNOCKOUT);
        assertThat(response.getSwissRounds()).isEqualTo(5);
        assertThat(response.getPlayoffTeams()).isEqualTo(8);
        assertThat(response.getGrandFinalReset()).isTrue();
    }

    @Test
    void toResponse_whenFormatHasTiebreakers_thenReturnThemAsAList() {
        EventFormatsEntity entity = new EventFormatsEntity();
        entity.setTiebreakers("WINS,HEAD_TO_HEAD");

        EventFormatResponse response = mapper.toResponse(entity);

        assertThat(response.getTiebreakers()).containsExactly(TiebreakerCriterion.WINS, TiebreakerCriterion.HEAD_TO_HEAD);
    }

    @Test
    void toResponse_whenTiebreakersWereNeverSet_thenReturnNullNotAnEmptyList() {
        assertThat(mapper.toResponse(new EventFormatsEntity()).getTiebreakers()).isNull();
    }

    @Test
    void toResponse_whenTiebreakersAreEmpty_thenReturnAnEmptyList() {
        EventFormatsEntity entity = new EventFormatsEntity();
        entity.setTiebreakers("");

        assertThat(mapper.toResponse(entity).getTiebreakers()).isEmpty();
    }

    @Test
    void updateEntity_whenARuleIsNotSent_thenKeepTheStoredOne() {
        EventFormatsEntity entity = new EventFormatsEntity();
        entity.setStructure(EventFormatStructure.ROUND_ROBIN);
        entity.setPointsForWin(3);
        entity.setTiebreakers("GOAL_DIFFERENCE");
        UpdateEventFormatRequest request = new UpdateEventFormatRequest();
        request.setPointsForDraw(1);

        mapper.updateEntity(entity, request);

        assertThat(entity.getStructure()).isEqualTo(EventFormatStructure.ROUND_ROBIN);
        assertThat(entity.getPointsForWin()).isEqualTo(3);
        assertThat(entity.getPointsForDraw()).isEqualTo(1);
        assertThat(entity.getTiebreakers()).isEqualTo("GOAL_DIFFERENCE");
    }

    @Test
    void updateEntity_whenAnEmptyTiebreakerListIsSent_thenClearTheStoredOnes() {
        EventFormatsEntity entity = new EventFormatsEntity();
        entity.setTiebreakers("GOAL_DIFFERENCE");
        UpdateEventFormatRequest request = new UpdateEventFormatRequest();
        request.setTiebreakers(List.of());

        mapper.updateEntity(entity, request);

        assertThat(entity.getTiebreakers()).isEmpty();
    }
}
