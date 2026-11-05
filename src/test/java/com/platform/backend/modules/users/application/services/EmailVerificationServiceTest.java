package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.domain.entities.EmailVerificationTokenEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.exception.EmailVerificationError;
import com.platform.backend.modules.users.domain.irepositories.IEmailVerificationTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.presentation.requests.VerifyEmailRequest.VerifyEmailRequest;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.token.OpaqueTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "ana@email.com";

    @Mock private IUserRepository userRepository;
    @Mock private IEmailVerificationTokenRepository tokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private UsersEntity user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "expirationHours", 48L);
        user = new UsersEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setFirstName("Ana");
    }

    private static VerifyEmailRequest request(String token) {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(token);
        return request;
    }

    // ─── sendVerificationLink ───────────────────────────────────────────────────

    @Test
    void sendVerificationLink_whenCalled_thenTheTokenIsStoredOnlyAsAHashAndTheRawOneIsEmailed() {
        emailVerificationService.sendVerificationLink(user);

        ArgumentCaptor<EmailVerificationTokenEntity> stored = ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
        ArgumentCaptor<String> emailed = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).save(stored.capture());
        verify(emailService).sendEmailVerification(eq(EMAIL), eq("Ana"), emailed.capture());
        assertThat(stored.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(stored.getValue().getTokenHash()).isEqualTo(OpaqueTokens.hash(emailed.getValue()));
        assertThat(stored.getValue().getTokenHash()).isNotEqualTo(emailed.getValue());
        assertThat(stored.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusHours(47));
    }

    @Test
    void sendVerificationLink_whenTheAccountHadALink_thenTheOldOneIsRemovedBeforeTheNewOneIsSaved() {
        emailVerificationService.sendVerificationLink(user);

        var order = inOrder(tokenRepository);
        order.verify(tokenRepository).deleteByUserId(user.getId());
        order.verify(tokenRepository).save(any(EmailVerificationTokenEntity.class));
    }

    // ─── resendVerificationLink ─────────────────────────────────────────────────

    @Test
    void resendVerificationLink_whenTheEmailIsNotConfirmed_thenANewLinkIsSent() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        emailVerificationService.resendVerificationLink("  Ana@Email.com ");

        verify(emailService).sendEmailVerification(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void resendVerificationLink_whenTheEmailIsAlreadyConfirmed_thenNothingIsSent() {
        user.setEmailVerifiedAt(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        emailVerificationService.resendVerificationLink(EMAIL);

        verify(emailService, never()).sendEmailVerification(anyString(), anyString(), anyString());
        verify(tokenRepository, never()).save(any());
    }

    // ─── verify ─────────────────────────────────────────────────────────────────

    private EmailVerificationTokenEntity givenAUsableLinkFor(String rawToken, LocalDateTime expiresAt) {
        EmailVerificationTokenEntity link = new EmailVerificationTokenEntity();
        link.setUserId(user.getId());
        link.setTokenHash(OpaqueTokens.hash(rawToken));
        link.setExpiresAt(expiresAt);
        when(tokenRepository.findByTokenHashAndUnusedForUpdate(OpaqueTokens.hash(rawToken))).thenReturn(Optional.of(link));
        return link;
    }

    @Test
    void verify_whenTheLinkIsValid_thenTheEmailIsConfirmedAndTheLinkIsUsed() {
        EmailVerificationTokenEntity link = givenAUsableLinkFor("raw", LocalDateTime.now().plusHours(1));
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));

        emailVerificationService.verify(request("raw"));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(link.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(link);
    }

    @Test
    void verify_whenTheLinkIsUnknownOrAlreadyUsed_thenItIsRefusedAsInvalid() {
        when(tokenRepository.findByTokenHashAndUnusedForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verify(request("nope")))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors())
                                .containsOnly(entry("token", EmailVerificationError.INVALID_OR_EXPIRED.name())));
        verify(userRepository, never()).save(any());
    }

    @Test
    void verify_whenTheLinkHasExpired_thenItIsRefusedAndTheEmailStaysUnconfirmed() {
        givenAUsableLinkFor("old", LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> emailVerificationService.verify(request("old")))
                .isInstanceOf(FieldValidationException.class);

        assertThat(user.isEmailVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void verify_whenTheAccountWasDeletedMeanwhile_thenItIsRefusedAsInvalid() {
        givenAUsableLinkFor("raw", LocalDateTime.now().plusHours(1));
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verify(request("raw")))
                .isInstanceOf(FieldValidationException.class);
    }

    // ─── requireVerified ────────────────────────────────────────────────────────

    @Test
    void requireVerified_whenTheEmailIsConfirmed_thenItPasses() {
        when(userRepository.hasVerifiedEmail(EMAIL)).thenReturn(true);

        emailVerificationService.requireVerified("Ana@Email.com");
    }

    @Test
    void requireVerified_whenTheEmailIsNotConfirmed_thenItIsRefusedWithAForbiddenCode() {
        when(userRepository.hasVerifiedEmail(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> emailVerificationService.requireVerified(EMAIL))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessError.EMAIL_NOT_VERIFIED.name());
    }
}
