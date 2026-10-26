package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.PasswordResetTokenEntity;
import com.platform.backend.modules.users.domain.irepositories.IPasswordResetTokenRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.PasswordResetTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepository implements IPasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;

    @Override
    public PasswordResetTokenEntity save(PasswordResetTokenEntity token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<PasswordResetTokenEntity> findByTokenHashAndUnusedForUpdate(String tokenHash) {
        return jpaRepository.findByTokenHashAndUsedAtIsNullForUpdate(tokenHash);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteByUserIdAndIdNot(UUID userId, UUID id) {
        jpaRepository.deleteByUserIdAndIdNot(userId, id);
    }

    @Override
    public void deleteExpiredBefore(LocalDateTime expiresAt) {
        jpaRepository.deleteByExpiresAtBefore(expiresAt);
    }
}
