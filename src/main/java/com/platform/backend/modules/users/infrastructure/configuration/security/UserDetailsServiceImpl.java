package com.platform.backend.modules.users.infrastructure.configuration.security;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IUsersRolesRepository usersRolesRepository;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    /**
     * Spring Security calls this during login to compare passwords.
     * Must return the stored BCrypt hash via getPassword() — AuthenticatedUser
     * returns null intentionally (JWT principal), so we use the standard User here.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UsersEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String authority = isAdmin(user) ? "ROLE_ADMIN" : "ROLE_USER";

        return new User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    /**
     * Admin is determined solely by whether the user has an active users_roles
     * row pointing at the fixed admin role id configured in app.security.admin-role-id —
     * never by role name.
     */
    private boolean isAdmin(UsersEntity user) {
        if (!StringUtils.hasText(adminRoleId)) {
            return false;
        }
        return usersRolesRepository.existsActiveByUserAndRole(user.getId(), UUID.fromString(adminRoleId));
    }
}
