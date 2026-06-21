package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {

    private final UsersJpaRepository usersJpaRepository;

    @Override
    public UsersEntity save(UsersEntity user) {
        return usersJpaRepository.save(user);
    }

    @Override
    public List<UsersEntity> findAllActive() {
        return usersJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<UsersEntity> findById(UUID id) {
        return usersJpaRepository.findById(id);
    }

    @Override
    public Optional<UsersEntity> findByEmail(String email) {
        return usersJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<UsersEntity> findByUsername(String username) {
        return usersJpaRepository.findByUsername(username);
    }

    @Override
    public Optional<UsersEntity> findActiveById(UUID id) {
        return usersJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersJpaRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public void hardDelete(UsersEntity user) {
        usersJpaRepository.delete(user);
    }
}

