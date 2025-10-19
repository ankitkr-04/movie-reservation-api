package com.moviereservation.api.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.domain.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipalImpl implements UserPrincipal {
    private UUID userId;
    private String email;
    private String password;
    private UserRole role;

    public static UserPrincipal create(final User user) {
        return new UserPrincipalImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        // For simplicity, we assume accounts never expire
        return true;
    }

    /**
     * Account locking check.
     * Return true = account never locked (no locking logic implemented).
     * 
     * Future enhancement: Lock account after 5 failed login attempts.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credentials expiration check.
     * Return true = password never expires (no expiration logic implemented).
     * 
     * Future enhancement: Force password change every 90 days.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Account enabled check.
     * Return true = account always enabled (no email verification implemented).
     * 
     * Future enhancement: Add email verification flow.
     * - User registers → enabled = false
     * - User clicks email link → enabled = true
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

}
