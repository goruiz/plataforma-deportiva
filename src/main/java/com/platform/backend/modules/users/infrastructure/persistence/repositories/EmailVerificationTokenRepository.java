package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.EmailVerificationTokenEntity;
import com.platform.backend.modules.users.domain.irepositories.IEmailVerificationTokenRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.EmailVerificationTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepository implements IEmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpaRepository;

    @Override
    public EmailVerificationTokenEntity save(EmailVerificationTokenEntity token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<EmailVerificationTokenEntity> findByTokenHashAndUnusedForUpdate(String tokenHash) {
        return jpaRepository.findByTokenHashAndUsedAtIsNullForUpdate(tokenHash);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteAllOfUser(userId);
    }

    @Override
    public void deleteExpiredBefore(LocalDateTime expiresAt) {
        jpaRepository.deleteByExpiresAtBefore(expiresAt);
    }
}
