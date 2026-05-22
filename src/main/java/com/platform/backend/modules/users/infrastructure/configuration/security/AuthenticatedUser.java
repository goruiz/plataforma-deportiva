package com.platform.backend.modules.users.infrastructure.configuration.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    private final UUID id;

    private final String firstName;
    private final String middleName;

    private final String lastName;
    private final String secondLastName;

    private final String email;

    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(
            UUID id,
            String firstName,
            String middleName,
            String lastName,
            String secondLastName,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;

        this.firstName = firstName;
        this.middleName = middleName;

        this.lastName = lastName;
        this.secondLastName = secondLastName;

        this.email = email;

        this.authorities = authorities;
    }

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

    public String getEmail() {
        return email;
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

    public static AuthenticatedUser from(
            UUID id,
            String firstName,
            String middleName,
            String lastName,
            String secondLastName,
            String email,
            String role
    ) {

        return new AuthenticatedUser(
                id,
                firstName,
                middleName,
                lastName,
                secondLastName,
                email,
                List.of(() -> role)
        );
    }
}