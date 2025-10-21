package com.moviereservation.api.constant;

public class PaginationDefaults {

    private PaginationDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

}
