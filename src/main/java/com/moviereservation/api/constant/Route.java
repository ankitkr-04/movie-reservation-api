package com.moviereservation.api.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Route {

    // Base paths only
    public static final String API_V1 = "/api/v1";
    public static final String ADMIN = API_V1 + "/admin";

    // Resource paths
    public static final String AUTH = API_V1 + "/auth";
    public static final String MOVIES = API_V1 + "/movies";
    public static final String RESERVATIONS = API_V1 + "/reservations";
    public static final String SHOWTIMES = API_V1 + "/showtimes";

    // Admin resources
    public static final String ADMIN_MOVIES = ADMIN + "/movies";
    public static final String ADMIN_RESERVATIONS = ADMIN + "/reservations";
    public static final String ADMIN_SHOWTIMES = ADMIN + "/showtimes";
}