package com.moviereservation.api.web.mapper;

import org.mapstruct.*;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.web.dto.request.movie.CreateMovieRequest;
import com.moviereservation.api.web.dto.request.movie.UpdateMovieRequest;
import com.moviereservation.api.web.dto.response.movie.MovieResponse;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MovieResponse toCustomerResponse(Movie movie);

    MovieResponse toAdminResponse(Movie movie);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(req.getStatus() != null ? req.getStatus() : com.moviereservation.api.domain.enums.MovieStatus.COMING_SOON)")
    void toEntity(CreateMovieRequest req, @MappingTarget Movie movie);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "id", ignore = true)

    void toEntity(UpdateMovieRequest req, @MappingTarget Movie movie);

}
