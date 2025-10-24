package com.moviereservation.api.security;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/health/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**");

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {

        final String clientIp = getClientIP(request);
        final Bucket bucket = cache.computeIfAbsent(clientIp, ip -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            new ObjectMapper().writeValue(response.getWriter(), Map.of(
                    "status", "error",
                    "message",
                    "Rate limit exceeded. Maximum " + MAX_REQUESTS_PER_MINUTE + " requests per minute allowed."));
        }
    }

    private Bucket createBucket() {
        final Bandwidth limit = Bandwidth.simple(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIP(final HttpServletRequest request) {
        return request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        return EXCLUDE_PATHS.stream().anyMatch(p -> PatternMatchUtils.simpleMatch(p, path));
    }

}
