package com.moviereservation.api.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.moviereservation.api.security.UserPrincipal;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfiguration {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return new AuditorAwareImpl();
    }

    public static class AuditorAwareImpl implements AuditorAware<UUID> {

        @Override
        @NonNull
        public Optional<UUID> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.empty();
            }

            if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return Optional.of(userPrincipal.getUserId());
            }

            return Optional.empty();
        }
    }
}