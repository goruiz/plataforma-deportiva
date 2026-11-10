package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IAccountSuspensionService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.exception.AccountSuspensionError;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountSuspensionService implements IAccountSuspensionService {

    private final IUserRepository userRepository;
    private final PlatformOwnerRole platformOwnerRole;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse changeSuspension(UUID userId, boolean suspended) {
        // Read through the organization filter: an administrator cannot reach an account of another organization.
        UsersEntity account = userRepository.findActiveById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        requireAllowedToChange(account);

        if (suspended) {
            suspend(account);
        } else {
            account.setSuspendedAt(null);
        }
        return userMapper.toUserResponse(userRepository.save(account));
    }

    private void requireAllowedToChange(UsersEntity account) {
        if (account.getId().equals(CurrentCaller.require().getId())) {
            throw new IllegalStateException(AccountSuspensionError.CANNOT_SUSPEND_YOURSELF.name());
        }
        if (platformOwnerRole.isOwner(account.getId()) && !CurrentTenant.isPlatformOwner()) {
            throw new AccessDeniedException(AccountSuspensionError.PLATFORM_OWNER_PROTECTED.name());
        }
    }

    /**
     * The sessions end by moving {@code tokens_valid_after}, not only by the suspension itself: otherwise reactivating
     * the account would bring back every token it had. Already suspended keeps its first moment.
     */
    private void suspend(UsersEntity account) {
        if (account.isSuspended()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        account.setSuspendedAt(now);
        account.setTokensValidAfter(now);
    }
}
