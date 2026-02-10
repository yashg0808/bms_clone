package com.bookmyshow.shared.geosharding;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Set;

/**
 * CityRoutingInterceptor — Reads the X-City-ID header and maps it to a region.
 *
 * City → Region mapping is configurable. For this setup:
 *   - North cities → "north" database
 *   - South cities → "south" database
 *
 * If no X-City-ID header is present, defaults to "north".
 *
 * IMPORTANT: Clears the ThreadLocal in afterCompletion() to prevent
 * thread-pool contamination.
 */
@Slf4j
public class CityRoutingInterceptor implements HandlerInterceptor {

    private static final String CITY_HEADER = "X-City-ID";

    private final Map<String, String> cityToRegion;

    /**
     * @param cityToRegion mapping of city IDs to region names ("north" or "south")
     */
    public CityRoutingInterceptor(Map<String, String> cityToRegion) {
        this.cityToRegion = cityToRegion;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cityId = request.getHeader(CITY_HEADER);

        if (cityId != null && !cityId.isBlank()) {
            String region = cityToRegion.getOrDefault(cityId, GeoShardingContext.DEFAULT_REGION);
            GeoShardingContext.setRegion(region);
            log.debug("Geo-shard routed: cityId={} → region={}", cityId, region);
        } else {
            GeoShardingContext.setRegion(GeoShardingContext.DEFAULT_REGION);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        GeoShardingContext.clear();
    }
}
