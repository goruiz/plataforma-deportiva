package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.UsersRolesEntity;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersRolesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
