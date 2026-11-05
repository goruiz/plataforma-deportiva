package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.EmailVerificationTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM EmailVerificationTokenEntity token "
            + "WHERE token.tokenHash = :tokenHash AND token.usedAt IS NULL")
    Optional<EmailVerificationTokenEntity> findByTokenHashAndUsedAtIsNullForUpdate(
            @Param("tokenHash") String tokenHash);

    // A bulk delete runs at once. A derived delete is queued and Hibernate runs inserts first, so replacing the unused
    // link of an account in one transaction would meet the unique index (one unused link per account) before it left.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM EmailVerificationTokenEntity token WHERE token.userId = :userId")
    void deleteAllOfUser(@Param("userId") UUID userId);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
