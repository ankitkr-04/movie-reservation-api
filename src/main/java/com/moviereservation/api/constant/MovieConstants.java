package com.moviereservation.api.constant;

import lombok.experimental.UtilityClass;

/**
 * Movie and showtime-related constants.
 */
@UtilityClass
public final class MovieConstants {
    
    // Default values for showtime/movie
    public static final short DEFAULT_AVAILABLE_SEATS = 120;
    public static final short MIN_SEAT_COUNT = 1;
    public static final short MAX_SEAT_COUNT = 500;
    
    // Movie duration constraints (in minutes)
    public static final int MIN_MOVIE_DURATION = 1;
    public static final int MAX_MOVIE_DURATION = 600; // 10 hours
}
