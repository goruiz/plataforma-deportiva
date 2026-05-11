package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;

import java.util.Optional;

public interface IUserRepository {

    UsersEntity save(UsersEntity user);

    Optional<UsersEntity> findByEmail(String email);

    Optional<UsersEntity> findByUsername(String username);
}
