package com.platform.backend.modules.courts.application.mappers;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtMapperTest {

    private final CourtMapper mapper = Mappers.getMapper(CourtMapper.class);

    @Test
    void toResponse_whenCourtBelongsToAnOrganization_thenExposeIt() {
        UUID organizationId = UUID.randomUUID();
        CourtsEntity court = new CourtsEntity();
        court.setName("Cancha 1");
        court.setOrganizationId(organizationId);

        CourtResponse response = mapper.toResponse(court);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void toResponse_whenCourtIsThePlatformOwners_thenOrganizationIsNull() {
        CourtsEntity court = new CourtsEntity();
        court.setName("Cancha del propietario");

        CourtResponse response = mapper.toResponse(court);

        assertThat(response.getOrganizationId()).isNull();
    }

    @Test
    void toResponse_whenCourtIsInAComplex_thenKeepTheComplexIdAndName() {
        UUID complexId = UUID.randomUUID();
        SportComplexesEntity complex = new SportComplexesEntity();
        complex.setId(complexId);
        complex.setName("Complejo Norte");
        CourtsEntity court = new CourtsEntity();
        court.setName("Cancha 2");
        court.setSportComplex(complex);

        CourtResponse response = mapper.toResponse(court);

        assertThat(response.getSportComplexId()).isEqualTo(complexId);
        assertThat(response.getSportComplexName()).isEqualTo("Complejo Norte");
    }
}
