package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository {

    UsersEntity save(UsersEntity user);

    List<UsersEntity> findAllActive();

    List<UsersEntity> findAllActiveWithRole(UUID roleId);

    List<UsersEntity> findAllActiveWithoutRole(UUID roleId);

    Optional<UsersEntity> findById(UUID id);

    Optional<UsersEntity> findByEmail(String email);

    Optional<UsersEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<UsersEntity> findActiveById(UUID id);

    void hardDelete(UsersEntity user);
}
