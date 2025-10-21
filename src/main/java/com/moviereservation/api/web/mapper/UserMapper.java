package com.moviereservation.api.web.mapper;

import org.mapstruct.Mapper;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.web.dto.response.user.UserResponse;

/**
 * MapStruct mapper for User entity conversions.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    
    /**
     * Convert User entity to UserResponse DTO.
     */
    UserResponse toResponse(User user);
}

