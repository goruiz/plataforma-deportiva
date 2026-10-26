package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.domain.entities.PasswordResetTokenEntity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IPasswordResetTokenRepository {

    PasswordResetTokenEntity save(PasswordResetTokenEntity token);

    Optional<PasswordResetTokenEntity> findByTokenHashAndUnusedForUpdate(String tokenHash);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndIdNot(UUID userId, UUID id);

    void deleteExpiredBefore(LocalDateTime expiresAt);
}
