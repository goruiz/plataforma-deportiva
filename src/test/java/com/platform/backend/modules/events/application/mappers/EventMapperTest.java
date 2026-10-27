package com.platform.backend.modules.events.application.mappers;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    private EventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(EventMapper.class);
        // The spring component model injects the nested format mapper; here it is wired by hand.
        ReflectionTestUtils.setField(mapper, "eventFormatMapper", Mappers.getMapper(EventFormatMapper.class));
    }

    @Test
    void toResponse_whenEventBelongsToAnOrganization_thenExposeIt() {
        UUID organizationId = UUID.randomUUID();
        EventsEntity event = new EventsEntity();
        event.setName("Liga Norte");
        event.setOrganizationId(organizationId);

        EventResponse response = mapper.toResponse(event);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void toResponse_whenEventIsThePlatformOwners_thenOrganizationIsNull() {
        EventsEntity event = new EventsEntity();
        event.setName("Copa de la plataforma");

        EventResponse response = mapper.toResponse(event);

        assertThat(response.getOrganizationId()).isNull();
    }
}
