package com.platform.backend.modules.teams.domain.irepositories;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITeamRepository {

    TeamsEntity save(TeamsEntity team);

    Optional<TeamsEntity> findById(UUID id);

    List<TeamsEntity> findAllActive();
    
    List<TeamsEntity> findAllActiveByIds(Collection<UUID> ids);


    Optional<TeamsEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    boolean existsByNameExcludingId(String name, UUID id);

    boolean existsActiveByCategoryId(UUID categoryId);

    /** A join code identifies one team in the whole platform, so this looks at every organization, not only the caller's. */
    boolean existsActiveByJoinCode(String joinCode);

    /** The active team that has this join code, whatever organization it belongs to: whoever types it has none yet. */
    Optional<TeamsEntity> findActiveByJoinCode(String joinCode);

    boolean existsActiveByOrganizationId(UUID organizationId);

    void delete(TeamsEntity team);

    void hardDelete(TeamsEntity team);

}
