package com.moviereservation.api.security;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.moviereservation.api.domain.enums.UserRole;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long jwtExpirationInMs;

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") final String jwtSecret,
            @Value("${app.security.jwt.expiration-ms}") final long jwtExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationInMs = jwtExpirationMs;
    }

    public String generateToken(final Authentication authentication) {
        final UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        final Date now = new Date();
        final Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(userPrincipal.getUserId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getRole())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();

    }

    public String generateToken(final UUID userId, final String email, final UserRole role) {
        final Date now = new Date();
        final Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public Long getExpiryDuration() {
        return jwtExpirationInMs;
    }

    public UUID getUserIdFromToken(final String token) {
        final Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public Claims getClaimsFromToken(final String token) {
        final var claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims;
    }

    public boolean validateToken(final String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (final SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (final MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (final ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (final UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (final IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}
