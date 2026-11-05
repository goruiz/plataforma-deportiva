package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IEmailVerificationService;
import com.platform.backend.modules.users.domain.entities.EmailVerificationTokenEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.exception.EmailVerificationError;
import com.platform.backend.modules.users.domain.irepositories.IEmailVerificationTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.requests.VerifyEmailRequest.VerifyEmailRequest;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.token.OpaqueTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService implements IEmailVerificationService {

    private static final String TOKEN_FIELD = "token";

    private final IUserRepository userRepository;
    private final IEmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.security.email-verification-expiration-hours:48}")
    private long expirationHours;

    @Override
    @Transactional
    public void sendVerificationLink(UsersEntity user) {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now());
        tokenRepository.deleteByUserId(user.getId());

        String token = OpaqueTokens.generate();
        EmailVerificationTokenEntity verification = new EmailVerificationTokenEntity();
        verification.setUserId(user.getId());
        verification.setTokenHash(OpaqueTokens.hash(token));
        verification.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        tokenRepository.save(verification);

        emailService.sendEmailVerification(user.getEmail(), user.getFirstName(), token);
    }

    @Override
    @Transactional
    public void resendVerificationLink(String email) {
        UsersEntity user = userRepository.findByEmail(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (!user.isEmailVerified()) {
            sendVerificationLink(user);
        }
    }

    @Override
    @Transactional
    public void verify(VerifyEmailRequest request) {
        EmailVerificationTokenEntity verification = tokenRepository
                .findByTokenHashAndUnusedForUpdate(OpaqueTokens.hash(request.getToken()))
                .filter(saved -> saved.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(this::invalidOrExpired);
        UsersEntity user = userRepository.findActiveById(verification.getUserId())
                .orElseThrow(this::invalidOrExpired);

        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        verification.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verification);
    }

    @Override
    public void requireVerified(String email) {
        if (!userRepository.hasVerifiedEmail(EmailNormalizer.normalize(email))) {
            throw new AccessDeniedException(AccessError.EMAIL_NOT_VERIFIED.name());
        }
    }

    private FieldValidationException invalidOrExpired() {
        return new FieldValidationException(TOKEN_FIELD, EmailVerificationError.INVALID_OR_EXPIRED.name());
    }
}
