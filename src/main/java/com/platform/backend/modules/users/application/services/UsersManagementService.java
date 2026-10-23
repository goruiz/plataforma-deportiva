package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.organizations.domain.irepositories.IOrganizationRepository;
import com.platform.backend.modules.users.application.iservices.IUsersManagementService;
import com.platform.backend.modules.users.application.mappers.UserMapper;
import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.enums.UserSection;
import com.platform.backend.modules.users.domain.filters.UserListFilter;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UserListItemResponse.UserListItemResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.domain.tenancy.TenantScope;
import com.platform.backend.shared.infraestructure.email.EmailService;
import com.platform.backend.shared.infraestructure.tenancy.CurrentTenant;
import com.platform.backend.shared.infraestructure.security.password.TemporaryPasswordGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersManagementService implements IUsersManagementService {

    private final IUserRepository userRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final IOrganizationRepository organizationRepository;
    private final PlatformOwnerRole platformOwnerRole;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    /** Only the page asked for is read, and the roles of its accounts in one more query (never one per account). */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserListItemResponse> getUsers(UserSection section, PageQuery pageQuery, List<String> searchTerms) {
        PageResult<UsersEntity> page = listFilterOf(section, searchTerms)
                .map(filter -> userRepository.findActivePage(filter, pageQuery))
                .orElseGet(() -> PageResult.empty(pageQuery));
        return PageResponse.from(withRoles(page));
    }

    /**
     * Three counts are enough: the sections split every account (the owner role wins over the administrator one), so
     * the app users are what is left of the total.
     */
    @Override
    @Transactional(readOnly = true)
    public UserCountsResponse countUsersBySection(List<String> searchTerms) {
        long total = countOf(UserSection.ALL, searchTerms);
        long owners = countOf(UserSection.OWNERS, searchTerms);
        long admins = countOf(UserSection.ADMINS, searchTerms);
        return new UserCountsResponse(owners, admins, total - owners - admins);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    /**
     * The admin never chooses the password: a temporary one is generated and emailed to the new user.
     * Transactional on purpose — if the email can't be sent the user is rolled back, so no account
     * is left behind whose password nobody knows.
     */
    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email already in use");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DataIntegrityViolationException("Username already in use");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        UsersEntity user = userMapper.toEntity(request);
        user.setOrganizationId(resolveOrganizationOfNewUser(request.getOrganizationId()));
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        UsersEntity savedUser = userRepository.save(user);

        emailService.sendUserCreatedWithTemporaryPassword(
                savedUser.getEmail(), savedUser.getFirstName(), temporaryPassword);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email already in use");
        }
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())
                && userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DataIntegrityViolationException("Username already in use");
        }

        userMapper.updateEntity(user, request);
        moveToOrganization(user, request.getOrganizationId());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public void softDelete(UUID id) {
        UsersEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void hardDelete(UUID id) {
        UsersEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        userRepository.hardDelete(user);
    }

    /**
     * An administrator of an organization creates staff inside it, whatever the request says. Whoever sees everything
     * (the platform owner) chooses the organization, or none: an account without one belongs to the platform.
     */
    private UUID resolveOrganizationOfNewUser(UUID requestedOrganizationId) {
        TenantScope scope = CurrentTenant.scope();
        if (scope.restricted()) {
            return scope.organizationId();
        }
        if (requestedOrganizationId != null) {
            requireOrganizationToExist(requestedOrganizationId);
        }
        return requestedOrganizationId;
    }

    /**
     * Only whoever sees everything (the platform owner) moves an account to another organization: an administrator of
     * an organization only ever sees their own staff, so for them the field is ignored, as when creating. A null leaves
     * the account where it is, like every other field of an update. The account's open sessions keep the old
     * organization in their token until they sign in again.
     */
    private void moveToOrganization(UsersEntity user, UUID requestedOrganizationId) {
        if (requestedOrganizationId == null
                || CurrentTenant.scope().restricted()
                || requestedOrganizationId.equals(user.getOrganizationId())) {
            return;
        }
        requireOrganizationToExist(requestedOrganizationId);
        user.setOrganizationId(requestedOrganizationId);
    }

    private void requireOrganizationToExist(UUID organizationId) {
        organizationRepository.findActiveById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + organizationId));
    }

    /**
     * What each section is made of, by role id (never by role name, as in login): the owners hold the owner role, the
     * administrators hold the administrator role and not the owner's, and the app users hold neither. A section whose
     * role is not configured has nobody, hence the empty result.
     */
    private Optional<UserListFilter> listFilterOf(UserSection section, List<String> searchTerms) {
        Optional<UUID> ownerRole = platformOwnerRole.id();
        Optional<UUID> administratorRole = configuredAdministratorRoleId();
        return switch (section) {
            case ALL -> Optional.of(new UserListFilter(List.of(), List.of(), searchTerms));
            case OWNERS -> ownerRole.map(owner -> new UserListFilter(List.of(owner), List.of(), searchTerms));
            case ADMINS -> administratorRole.map(administrator ->
                    new UserListFilter(List.of(administrator), ownerRole.stream().toList(), searchTerms));
            case APP_USERS -> Optional.of(new UserListFilter(List.of(), staffRoleIds(), searchTerms));
        };
    }

    private long countOf(UserSection section, List<String> searchTerms) {
        return listFilterOf(section, searchTerms).map(userRepository::countActive).orElse(0L);
    }

    /** The accounts of the page with their roles, read for all of them at once. */
    private PageResult<UserListItemResponse> withRoles(PageResult<UsersEntity> page) {
        List<UUID> userIds = page.content().stream().map(UsersEntity::getId).toList();
        Map<UUID, List<RolesEntity>> rolesByUserId = usersRolesRepository.findActiveRolesByUserIds(userIds);
        return page.map(user -> {
            UserListItemResponse item = userMapper.toUserListItemResponse(user);
            item.setRoles(rolesByUserId.getOrDefault(user.getId(), List.of()).stream()
                    .map(userMapper::toUserRoleResponse)
                    .toList());
            return item;
        });
    }

    private Optional<UUID> configuredAdministratorRoleId() {
        return StringUtils.hasText(adminRoleId) ? Optional.of(UUID.fromString(adminRoleId)) : Optional.empty();
    }

    /** The roles that make an account staff: the customers' administrator role and the owner's. */
    private List<UUID> staffRoleIds() {
        List<UUID> roleIds = new ArrayList<>();
        configuredAdministratorRoleId().ifPresent(roleIds::add);
        platformOwnerRole.id().ifPresent(roleIds::add);
        return roleIds;
    }
}

