package com.bookmyshow.shared.geosharding;

/**
 * ThreadLocal context for Geo-Sharding.
 *
 * Holds the current city region (derived from X-City-ID header)
 * so that the DataSource router can pick the correct database.
 *
 * The interceptor sets this on each request, and the DataSource reads it.
 * Always cleared in the interceptor's afterCompletion to prevent thread-pool leaks.
 */
public class GeoShardingContext {

    private static final ThreadLocal<String> CURRENT_REGION = new ThreadLocal<>();

    /** Default region when no X-City-ID header is present */
    public static final String DEFAULT_REGION = "north";

    public static void setRegion(String region) {
        CURRENT_REGION.set(region);
    }

    public static String getRegion() {
        String region = CURRENT_REGION.get();
        return region != null ? region : DEFAULT_REGION;
    }

    public static void clear() {
        CURRENT_REGION.remove();
    }
}
