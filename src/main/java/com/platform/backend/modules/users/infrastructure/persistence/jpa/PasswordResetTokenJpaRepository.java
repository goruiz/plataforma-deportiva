package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.PasswordResetTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM PasswordResetTokenEntity token "
            + "WHERE token.tokenHash = :tokenHash AND token.usedAt IS NULL")
    Optional<PasswordResetTokenEntity> findByTokenHashAndUsedAtIsNullForUpdate(
            @Param("tokenHash") String tokenHash);

    @Modifying
    void deleteByUserId(UUID userId);

    @Modifying
    void deleteByUserIdAndIdNot(UUID userId, UUID id);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
