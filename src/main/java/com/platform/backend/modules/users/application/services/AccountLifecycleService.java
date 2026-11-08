package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.users.application.iservices.IAccountLifecycleService;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.exception.AccountDeletionError;
import com.platform.backend.modules.users.domain.irepositories.IEmailVerificationTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IPasswordResetTokenRepository;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.DeleteAccountRequest.DeleteAccountRequest;
import com.platform.backend.shared.domain.utils.EmailNormalizer;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.token.OpaqueTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountLifecycleService implements IAccountLifecycleService {

    private static final String PASSWORD_FIELD = "password";

    /** Nobody can register under this domain (RFC 2606), so an erased account never collides with a real email. */
    private static final String ERASED_EMAIL_FORMAT = "deleted-%s@deleted.invalid";

    private final IUserRepository userRepository;
    private final IPlayerRepository playerRepository;
    private final IPlayerPhotoRepository playerPhotoRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final IEmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PlatformOwnerRole platformOwnerRole;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    /** What a match timeline shows where the name of the erased player was. */
    @Value("${app.accounts.erased-player-name:Cuenta eliminada}")
    private String erasedPlayerName;

    @Override
    @Transactional
    public void signOut(String email) {
        UsersEntity user = findAccount(email);
        user.setTokensValidAfter(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request, String email) {
        UsersEntity user = findAccount(email);
        if (isStaff(user)) {
            throw new IllegalStateException(AccountDeletionError.STAFF_ACCOUNT.name());
        }
        // A wrong password is a field error (400), never a 401: clients sign the user out on any 401.
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new FieldValidationException(PASSWORD_FIELD, AccountDeletionError.INCORRECT_PASSWORD.name());
        }

        erasePlayerOf(user);
        erase(user);
        passwordResetTokenRepository.deleteByUserId(user.getId());
        emailVerificationTokenRepository.deleteByUserId(user.getId());
    }

    /**
     * Erased, not just marked as deleted: the name and the email are gone from the row and the account cannot be used
     * again. What the player did (matches, goals) stays in the history under the erased name.
     */
    private void erase(UsersEntity user) {
        LocalDateTime now = LocalDateTime.now();
        user.setFirstName(null);
        user.setMiddleName(null);
        user.setLastName(null);
        user.setSecondLastName(null);
        user.setUsername(null);
        user.setEmail(erasedEmailOf(user.getId()));
        user.setPassword(passwordEncoder.encode(OpaqueTokens.generate()));
        user.setEmailVerifiedAt(null);
        user.setTokensValidAfter(now);
        user.setDeletedAt(now);
        userRepository.save(user);
    }

    /** Looked up by the email of the account, so it has to run before the email is erased. */
    private void erasePlayerOf(UsersEntity user) {
        playerRepository.findByEmail(user.getEmail())
                .filter(player -> player.getDeletedAt() == null)
                .ifPresent(this::erase);
    }

    private void erase(PlayersEntity player) {
        player.moveToTeam(null);
        player.setFirstName(erasedPlayerName);
        player.setLastName("");
        player.setEmail(erasedEmailOf(player.getId()));
        player.setPhone(null);
        player.setProfilePhotoUrl(null);
        player.setDeletedAt(LocalDateTime.now());
        playerRepository.save(player);
        playerPhotoRepository.deleteByPlayerId(player.getId());
    }

    private String erasedEmailOf(UUID id) {
        return ERASED_EMAIL_FORMAT.formatted(id);
    }

    private UsersEntity findAccount(String email) {
        return userRepository.findByEmail(EmailNormalizer.normalize(email))
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    /** An administrator is whoever holds the admin role (never by name) and the owner holds its own. */
    private boolean isStaff(UsersEntity user) {
        if (platformOwnerRole.isOwner(user.getId())) {
            return true;
        }
        return StringUtils.hasText(adminRoleId)
                && usersRolesRepository.existsActiveByUserAndRole(user.getId(), UUID.fromString(adminRoleId));
    }
}
