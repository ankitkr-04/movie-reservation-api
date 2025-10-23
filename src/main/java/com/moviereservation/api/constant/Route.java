package com.moviereservation.api.constant;

import lombok.experimental.UtilityClass;

/**
 * Application route constants.
 * Controllers should combine these base paths as needed (e.g., Route.ADMIN +
 * "/movies").
 */
@UtilityClass
public final class Route {

    // Base API version
    public static final String API_V1 = "/api/v1";

    // Public resources
    public static final String AUTH = API_V1 + "/auth";
    public static final String MOVIES = API_V1 + "/movies";
    public static final String RESERVATIONS = API_V1 + "/reservations";
    public static final String PAYMENTS = API_V1 + "/payments";
    public static final String SHOWTIMES = API_V1 + "/showtimes";

    // Swagger UI
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String API_DOCS = "/v3/api-docs/**";

    // Admin prefix only
    public static final String ADMIN = API_V1 + "/admin";
}