package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.service.UserService;
import com.moviereservation.api.web.dto.response.user.UserResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.UserMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Admin user management endpoints.
 * Allows admins to manage user roles and view user information.
 */
@RestController
@RequestMapping(Route.ADMIN + "/users")
@Tag(name = "Admin - Users", description = "User management for administrators")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Promote a user to ADMIN role.
     * Only existing ADMIN users can promote other users.
     */
    @PostMapping("/{userId}/promote")
    @Operation(summary = "Promote user to ADMIN", description = "Grants ADMIN role to a CUSTOMER user. This action is irreversible and should be used with caution.")
    public ResponseEntity<ApiResponse<UserResponse>> promoteToAdmin(
            @PathVariable @Parameter(description = "User UUID to promote") final UUID userId) {

        final User promotedUser = userService.promoteToAdmin(userId);

        return ResponseEntity.ok(
                ApiResponse.success("User promoted to ADMIN successfully", userMapper.toResponse(promotedUser)));
    }

    /**
     * Get user by ID.
     * Admin can view any user's details.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve detailed user information")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable @Parameter(description = "User UUID") final UUID userId) {

        final User user = userService.findById(userId);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userMapper.toResponse(user)));
    }

    /**
     * Browse all users with pagination.
     * Useful for user management and reporting.
     */
    @GetMapping
    @Operation(summary = "Browse all users", description = "Get paginated list of all registered users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> browseUsers(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt") final String sortBy,
            @RequestParam(defaultValue = "DESC") final String direction) {

        final Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size,
                org.springframework.data.domain.Sort.Direction.fromString(direction),
                sortBy);

        final Page<User> users = userService.findAll(pageable);
        final PagedResponse<UserResponse> response = PagedResponse.of(users, userMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", response));
    }
}
