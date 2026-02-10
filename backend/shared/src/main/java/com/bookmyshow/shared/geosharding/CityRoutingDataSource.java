package com.bookmyshow.shared.geosharding;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * CityRoutingDataSource — Routes SQL queries to region-specific databases
 * based on the X-City-ID header value stored in GeoShardingContext.
 *
 * Spring's AbstractRoutingDataSource calls determineCurrentLookupKey() before
 * every connection checkout from the pool. It returns the region key ("north" or "south"),
 * and Spring maps it to the corresponding DataSource from the targetDataSources map.
 *
 * Configuration in each service's application.yml:
 *   datasource:
 *     north:
 *       url: jdbc:postgresql://localhost:5432/bookmyshow_north
 *     south:
 *       url: jdbc:postgresql://localhost:5432/bookmyshow_south
 */
public class CityRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return GeoShardingContext.getRegion();
    }
}
