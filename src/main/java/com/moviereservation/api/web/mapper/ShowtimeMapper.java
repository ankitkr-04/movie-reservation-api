package com.moviereservation.api.web.mapper;

import org.mapstruct.*;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.web.dto.request.showtime.CreateShowtimeRequest;
import com.moviereservation.api.web.dto.request.showtime.UpdateShowtimeRequest;
import com.moviereservation.api.web.dto.response.showtime.ShowtimeAdminResponse;
import com.moviereservation.api.web.dto.response.showtime.ShowtimeCustomerResponse;

/**
 * MapStruct mapper for Showtime entity conversions.
 * Handles nested movie information mapping for both admin and customer views.
 */
@Mapper(componentModel = "spring")
public interface ShowtimeMapper {

    // ========== Customer Response Mappings ==========

    /**
     * Convert Showtime to customer response.
     * Uses nested MovieInfo instead of full Movie entity.
     */
    @Mapping(target = "movie", source = "showtime", qualifiedByName = "toCustomerMovieInfo")
    ShowtimeCustomerResponse toCustomerResponse(Showtime showtime);

    @Named("toCustomerMovieInfo")
    default ShowtimeCustomerResponse.MovieInfo toCustomerMovieInfo(final Showtime showtime) {
        final Movie movie = showtime.getMovie();
        return ShowtimeCustomerResponse.MovieInfo.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .genre(movie.getGenre().name())
                .rating(movie.getRating())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .build();
    }

    // ========== Admin Response Mappings ==========

    /**
     * Convert Showtime to admin response.
     * Includes status and timestamps for administrative purposes.
     */
    @Mapping(target = "movie", source = "showtime", qualifiedByName = "toAdminMovieInfo")
    ShowtimeAdminResponse toAdminResponse(Showtime showtime);

    @Named("toAdminMovieInfo")
    default ShowtimeAdminResponse.MovieInfo toAdminMovieInfo(final Showtime showtime) {
        final Movie movie = showtime.getMovie();
        return ShowtimeAdminResponse.MovieInfo.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .genre(movie.getGenre().name())
                .rating(movie.getRating())
                .status(movie.getStatus().name())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .build();
    }

    // ========== Request to Entity Mappings ==========

    /**
     * Map CreateShowtimeRequest to Showtime entity.
     * Movie and endTime must be set separately in service layer.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "availableSeatsCount", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Showtime toEntity(CreateShowtimeRequest request);

    /**
     * Map UpdateShowtimeRequest to existing Showtime entity.
     * Only updates non-null fields.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "availableSeatsCount", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateShowtimeRequest request, @MappingTarget Showtime showtime);
}