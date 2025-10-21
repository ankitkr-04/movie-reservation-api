package com.moviereservation.api.web.mapper;

import org.mapstruct.*;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.web.dto.request.movie.CreateMovieRequest;
import com.moviereservation.api.web.dto.request.movie.UpdateMovieRequest;
import com.moviereservation.api.web.dto.response.movie.MovieAdminResponse;
import com.moviereservation.api.web.dto.response.movie.MovieCustomerResponse;

/**
 * MapStruct mapper for Movie entity conversions.
 * Separates customer and admin responses for proper data hiding.
 */
@Mapper(componentModel = "spring")
public interface MovieMapper {

    /**
     * Convert Movie entity to customer-facing response.
     * Excludes administrative metadata.
     */
    MovieCustomerResponse toCustomerResponse(Movie movie);

    /**
     * Convert Movie entity to admin response.
     * Includes full administrative metadata.
     */
    MovieAdminResponse toAdminResponse(Movie movie);

    /**
     * Map CreateMovieRequest to Movie entity.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "status", expression = "java(req.getStatus() != null ? req.getStatus() : com.moviereservation.api.domain.enums.MovieStatus.COMING_SOON)")
    void toEntity(CreateMovieRequest req, @MappingTarget Movie movie);

    /**
     * Map UpdateMovieRequest to existing Movie entity.
     * Only updates non-null fields.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    void toEntity(UpdateMovieRequest req, @MappingTarget Movie movie);
}
