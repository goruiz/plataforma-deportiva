package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.EmailVerificationTokenEntity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IEmailVerificationTokenRepository {

    EmailVerificationTokenEntity save(EmailVerificationTokenEntity token);

    Optional<EmailVerificationTokenEntity> findByTokenHashAndUnusedForUpdate(String tokenHash);

    void deleteByUserId(UUID userId);

    void deleteExpiredBefore(LocalDateTime expiresAt);
}
