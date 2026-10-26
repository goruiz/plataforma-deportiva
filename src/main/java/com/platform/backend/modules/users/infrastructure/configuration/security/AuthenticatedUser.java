package com.platform.backend.modules.users.infrastructure.configuration.security;

import com.platform.backend.shared.domain.security.CallerPrincipal;
import com.platform.backend.shared.domain.tenancy.TenantPrincipal;
import com.platform.backend.shared.infraestructure.security.Authority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails, TenantPrincipal, CallerPrincipal {

    private final UUID id;

    private final String firstName;
    private final String middleName;

    private final String lastName;
    private final String secondLastName;

    private final String email;

    private final UUID organizationId;
    private final boolean platformOwner;

    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(
            UUID id,
            String firstName,
            String middleName,
            String lastName,
            String secondLastName,
            String email,
            UUID organizationId,
            boolean platformOwner,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;

        this.firstName = firstName;
        this.middleName = middleName;

        this.lastName = lastName;
        this.secondLastName = secondLastName;

        this.email = email;

        this.organizationId = organizationId;
        this.platformOwner = platformOwner;

        this.authorities = authorities;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public UUID getOrganizationId() {
        return organizationId;
    }

    @Override
    public boolean isPlatformOwner() {
        return platformOwner;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * No se usa password porque la autenticación
     * ya ocurrió previamente (JWT/stateless auth).
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Spring Security requiere username.
     * Usaremos el email como identificador principal.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Builds the caller from what the login put in the token. Every account is a USER; an administrator also gets
     * ADMIN and the platform owner also PLATFORM_OWNER.
     */
    public static AuthenticatedUser from(
            UUID id,
            String firstName,
            String middleName,
            String lastName,
            String secondLastName,
            String email,
            UUID organizationId,
            boolean admin,
            boolean platformOwner
    ) {
        return new AuthenticatedUser(
                id,
                firstName,
                middleName,
                lastName,
                secondLastName,
                email,
                organizationId,
                platformOwner,
                authoritiesFor(admin, platformOwner)
        );
    }

    private static List<GrantedAuthority> authoritiesFor(boolean admin, boolean platformOwner) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(Authority.USER.roleName()));
        if (admin) {
            authorities.add(new SimpleGrantedAuthority(Authority.ADMIN.roleName()));
        }
        if (platformOwner) {
            authorities.add(new SimpleGrantedAuthority(Authority.PLATFORM_OWNER.roleName()));
        }
        return authorities;
    }
}
