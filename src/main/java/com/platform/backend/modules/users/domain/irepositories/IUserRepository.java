package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;

import java.util.Optional;
import java.util.UUID;

public interface IUserRepository {

    UsersEntity save(UsersEntity user);

    Optional<UsersEntity> findById(UUID id);

    Optional<UsersEntity> findByEmail(String email);

    Optional<UsersEntity> findByUsername(String username);
}
