package com.moviereservation.api.constant;

public class Route {
    private Route() {
    }

    public static final String API_V1_BASE = "/api/v1";
    public static final String AUTH = API_V1_BASE + "/auth";
    public static final String REGISTER_USER = "/register";
    public static final String LOGIN_USER = "/login";
    public static final String CHANGE_USER_PASSWORD = "/change-password";

}
