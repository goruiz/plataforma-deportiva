package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {

    private final UsersJpaRepository usersJpaRepository;

    @Override
    public UsersEntity save(UsersEntity user) {
        return usersJpaRepository.save(user);
    }

    @Override
    public Optional<UsersEntity> findByEmail(String email) {
        return usersJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<UsersEntity> findByUsername(String username) {
        return usersJpaRepository.findByUsername(username);
    }
}
