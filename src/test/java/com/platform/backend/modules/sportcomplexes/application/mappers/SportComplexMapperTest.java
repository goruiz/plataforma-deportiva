package com.platform.backend.modules.sportcomplexes.application.mappers;

import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SportComplexMapperTest {

    private final SportComplexMapper mapper = Mappers.getMapper(SportComplexMapper.class);

    @Test
    void toResponse_whenComplexBelongsToAnOrganization_thenExposeIt() {
        UUID organizationId = UUID.randomUUID();
        SportComplexesEntity complex = new SportComplexesEntity();
        complex.setName("Complejo Norte");
        complex.setOrganizationId(organizationId);

        SportComplexResponse response = mapper.toResponse(complex);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void toResponse_whenComplexIsThePlatformOwners_thenOrganizationIsNull() {
        SportComplexesEntity complex = new SportComplexesEntity();
        complex.setName("Complejo del propietario");

        SportComplexResponse response = mapper.toResponse(complex);

        assertThat(response.getOrganizationId()).isNull();
    }
}
