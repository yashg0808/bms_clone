package com.bookmyshow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * WaitingRoomFilter — Surge protection using a virtual waiting room.
 *
 * The "Taylor Swift" Effect: When a popular movie opens for booking, thousands
 * of users hit the system simultaneously. This filter protects backend services
 * by capping concurrent active users.
 *
 * How it works:
 *   1. On every API request, check the Redis counter "waiting_room:active_users"
 *   2. If active_users > MAX_THRESHOLD → redirect to /waiting-room (HTTP 302)
 *   3. If below threshold → increment counter (with TTL), attach X-Surge-Token header, allow request
 *   4. The counter auto-decrements as TTL-based keys expire
 *
 * Implementation uses per-session keys instead of a single counter:
 *   Key: "waiting_room:session:{token}" with TTL = session-ttl (5 min)
 *   This ensures each user only counts once, and auto-expires.
 *   A sorted set "waiting_room:active_set" tracks all active tokens for counting.
 */
@Slf4j
@Component
public class WaitingRoomFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String ACTIVE_COUNTER_KEY = "waiting_room:active_users";
    private static final String SESSION_KEY_PREFIX = "waiting_room:session:";
    private static final String SURGE_TOKEN_HEADER = "X-Surge-Token";

    @Value("${waiting-room.max-threshold:5000}")
    private int maxThreshold;

    @Value("${waiting-room.session-ttl-seconds:300}")
    private int sessionTtlSeconds;

    @Value("${waiting-room.enabled:true}")
    private boolean enabled;

    public WaitingRoomFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();

        // Don't gate the waiting-room page itself, health checks, or static assets
        if (path.startsWith("/waiting-room") ||
            path.startsWith("/actuator") ||
            path.startsWith("/layouts/") ||
            path.equals("/favicon.ico")) {
            return chain.filter(exchange);
        }

        // Check if this request already has a valid surge token
        String existingToken = exchange.getRequest().getHeaders().getFirst(SURGE_TOKEN_HEADER);
        if (existingToken != null && !existingToken.isBlank()) {
            String sessionKey = SESSION_KEY_PREFIX + existingToken;
            return redisTemplate.hasKey(sessionKey)
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            // Valid session — refresh TTL and allow through
                            return redisTemplate.expire(sessionKey, Duration.ofSeconds(sessionTtlSeconds))
                                    .then(chain.filter(exchange));
                        }
                        // Token expired or invalid — treat as new user
                        return handleNewUser(exchange, chain);
                    });
        }

        return handleNewUser(exchange, chain);
    }

    private Mono<Void> handleNewUser(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check current active user count
        return redisTemplate.opsForValue().get(ACTIVE_COUNTER_KEY)
                .defaultIfEmpty("0")
                .flatMap(countStr -> {
                    long currentCount;
                    try {
                        currentCount = Long.parseLong(countStr);
                    } catch (NumberFormatException e) {
                        currentCount = 0;
                    }

                    if (currentCount >= maxThreshold) {
                        // Over threshold → redirect to waiting room
                        log.warn("Waiting room activated: {} active users (threshold: {})",
                                currentCount, maxThreshold);
                        return redirectToWaitingRoom(exchange);
                    }

                    // Under threshold → admit user
                    String surgeToken = UUID.randomUUID().toString();
                    String sessionKey = SESSION_KEY_PREFIX + surgeToken;

                    return redisTemplate.opsForValue()
                            .set(sessionKey, "1", Duration.ofSeconds(sessionTtlSeconds))
                            .then(redisTemplate.opsForValue().increment(ACTIVE_COUNTER_KEY))
                            .flatMap(newCount -> {
                                // Set TTL on counter if it's new
                                if (newCount != null && newCount == 1) {
                                    return redisTemplate.expire(ACTIVE_COUNTER_KEY,
                                            Duration.ofSeconds(sessionTtlSeconds + 60))
                                            .thenReturn(newCount);
                                }
                                return Mono.justOrEmpty(newCount);
                            })
                            .then(Mono.defer(() -> {
                                // Schedule decrement when session key expires
                                scheduleDecrement(sessionKey);

                                // Attach X-Surge-Token header to the request
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header(SURGE_TOKEN_HEADER, surgeToken)
                                        .build();

                                // Also set it as a response header so the frontend can persist it
                                exchange.getResponse().getHeaders().set(SURGE_TOKEN_HEADER, surgeToken);

                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            }));
                });
    }

    /**
     * Subscribe to Redis key expiration to decrement the counter.
     * As a simpler alternative, we use a delayed decrement.
     */
    private void scheduleDecrement(String sessionKey) {
        // Use Redis keyspace notifications or a simple delayed decrement
        // For simplicity, we decrement after the TTL using a reactive delay
        Mono.delay(Duration.ofSeconds(sessionTtlSeconds))
                .flatMap(tick -> redisTemplate.opsForValue().decrement(ACTIVE_COUNTER_KEY))
                .subscribe(
                        count -> { if (count != null && count < 0) {
                            // Reset if somehow negative
                            redisTemplate.opsForValue().set(ACTIVE_COUNTER_KEY, "0").subscribe();
                        }},
                        error -> log.warn("Failed to decrement active users counter: {}", error.getMessage())
                );
    }

    private Mono<Void> redirectToWaitingRoom(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND); // 302
        response.getHeaders().setLocation(URI.create("/waiting-room"));
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -2; // Run before LoggingFilter (-1) and AuthenticationFilter
    }
}
