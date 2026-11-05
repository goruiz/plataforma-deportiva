package com.platform.backend.modules.users.infrastructure.configuration.security;

import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.shared.domain.security.SessionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** One primary-key lookup per authenticated request: the price of a sign-out that really invalidates the token. */
@Component
@RequiredArgsConstructor
public class UserSessionChecker implements SessionChecker {

    private final IUserRepository userRepository;

    @Override
    public boolean isSessionValid(UUID userId, Instant issuedAt) {
        // The database stores local times (LocalDateTime.now()), so the instant is read in the same zone.
        return userRepository.hasSessionValidAt(userId, LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
    }
}
