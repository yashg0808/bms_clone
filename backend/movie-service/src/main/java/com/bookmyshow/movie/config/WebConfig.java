package com.bookmyshow.movie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * Serves static layout JSON files from the filesystem as /layouts/*.
 * In production, these would be served from a CDN (S3/CloudFront).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${layout.output-dir:./layouts}")
    private String layoutOutputDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve relative to the project directory (where pom.xml is)
        // This works whether started from project root or module directory
        File layoutDir = new File(layoutOutputDir);
        if (!layoutDir.isAbsolute()) {
            // Try relative to current directory first
            if (!layoutDir.exists()) {
                // Try relative to module directory (when running from project root)
                layoutDir = new File("backend/movie-service/" + layoutOutputDir);
            }
        }
        String absolutePath = layoutDir.getAbsoluteFile().toURI().toString();
        registry.addResourceHandler("/layouts/**")
                .addResourceLocations(absolutePath)
                .setCachePeriod(86400); // 24-hour browser cache (static data)
    }
}
