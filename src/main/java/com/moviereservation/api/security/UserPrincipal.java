package com.moviereservation.api.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.moviereservation.api.constant.SecurityConstants;
import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.domain.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spring Security UserDetails implementation for authenticated users.
 * Simplified from interface + implementation pattern to single class.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final UserRole role;

    /**
     * Factory method to create UserPrincipal from User entity.
     */
    public static UserPrincipal from(final User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
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
}
