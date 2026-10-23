package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersRolesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UsersRolesRepository implements IUsersRolesRepository {

    private final UsersRolesJpaRepository usersRolesJpaRepository;

    @Override
    public UsersRolesEntity save(UsersRolesEntity usersRoles) {
        return usersRolesJpaRepository.save(usersRoles);
    }

    @Override
    public Optional<UsersRolesEntity> findById(UUID id) {
        return usersRolesJpaRepository.findById(id);
    }

    @Override
    public List<UsersRolesEntity> findAllActive() {
        return usersRolesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<UsersRolesEntity> findActiveById(UUID id) {
        return usersRolesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(UsersRolesEntity usersRoles) {
        usersRolesJpaRepository.save(usersRoles);
    }

    @Override
    public void hardDelete(UsersRolesEntity usersRoles) {
        usersRolesJpaRepository.delete(usersRoles);
    }

    @Override
    public boolean existsActiveByUserAndRole(UUID userId, UUID roleId) {
        return usersRolesJpaRepository.existsByUser_IdAndRole_IdAndDeletedAtIsNull(userId, roleId);
    }

    @Override
    public List<UUID> findActiveRoleIdsByUserId(UUID userId) {
        return usersRolesJpaRepository.findActiveRoleIdsByUserId(userId);
    }

    @Override
    public List<UsersRolesEntity> findActiveByUserId(UUID userId) {
        return usersRolesJpaRepository.findAllByUser_IdAndDeletedAtIsNull(userId);
    }

    @Override
    public Map<UUID, List<RolesEntity>> findActiveRolesByUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<RolesEntity>> rolesByUserId = new HashMap<>();
        for (Object[] userAndRole : usersRolesJpaRepository.findActiveRolesByUserIds(userIds)) {
            rolesByUserId.computeIfAbsent((UUID) userAndRole[0], userId -> new ArrayList<>()).add((RolesEntity) userAndRole[1]);
        }
        return rolesByUserId;
    }
}
