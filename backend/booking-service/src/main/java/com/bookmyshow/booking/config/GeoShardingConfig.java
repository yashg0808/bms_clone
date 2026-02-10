package com.bookmyshow.booking.config;

import com.bookmyshow.shared.geosharding.CityRoutingDataSource;
import com.bookmyshow.shared.geosharding.CityRoutingInterceptor;
import com.bookmyshow.shared.geosharding.GeoShardingContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Geo-sharding DataSource configuration for booking-service.
 *
 * When enabled (geo-sharding.enabled=true), routes queries to
 * region-specific databases based on the X-City-ID header.
 *
 * Two logical datasources: "north" and "south", configured in application.yml.
 * For local dev, these point to different schemas in the same Postgres container.
 */
@Configuration
@ConditionalOnProperty(name = "geo-sharding.enabled", havingValue = "true")
public class GeoShardingConfig implements WebMvcConfigurer {

    @Value("#{${geo-sharding.city-region-map:{}}}")
    private Map<String, String> cityRegionMap;

    @Bean
    @ConfigurationProperties("geo-sharding.datasource.north")
    public DataSourceProperties northDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("geo-sharding.datasource.south")
    public DataSourceProperties southDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource northDataSource() {
        return northDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public DataSource southDataSource() {
        return southDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Primary
    @Bean
    public DataSource dataSource() {
        CityRoutingDataSource routingDataSource = new CityRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("north", northDataSource());
        targetDataSources.put("south", southDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(northDataSource());

        return routingDataSource;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CityRoutingInterceptor(cityRegionMap))
                .addPathPatterns("/api/**");
    }
}
