package com.platform.backend.modules.users.application.mappers;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void updateEntity_whenTheRequestCarriesAnOrganization_thenTheMapperNeverMovesTheAccount() {
        UUID currentOrganizationId = UUID.randomUUID();
        UsersEntity entity = new UsersEntity();
        entity.setFirstName("Ana");
        entity.setOrganizationId(currentOrganizationId);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Anita");
        request.setOrganizationId(UUID.randomUUID());

        mapper.updateEntity(entity, request);

        // Moving an account depends on who asks, so only the service does it.
        assertThat(entity.getFirstName()).isEqualTo("Anita");
        assertThat(entity.getOrganizationId()).isEqualTo(currentOrganizationId);
    }
}
