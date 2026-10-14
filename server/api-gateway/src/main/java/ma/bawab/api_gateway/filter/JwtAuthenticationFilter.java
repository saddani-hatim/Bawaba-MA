package ma.bawab.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import ma.bawab.api_gateway.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Gateway filter that validates the access token and rotates it on every
 * request.
 *
 * <p>
 * Redis is used to cache the old→new token mapping so that concurrent requests
 * arriving with the same old token all receive the SAME new token instead of
 * each generating a different one.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>Extract access token from cookie / Authorization header</li>
 * <li>Check Redis: was this token already rotated?
 * → Yes: reuse the cached new token (concurrent request scenario)</li>
 * <li>Validate JWT RS256 + extract claims</li>
 * <li>Generate new token, store old→new in Redis with grace-period TTL</li>
 * <li>Set new token cookie in response + inject headers downstream</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String ACCESS_ROTATED_PREFIX = "access:rotated:";

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessTokenCookieName;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${jwt.access-token-rotation-grace-ms:30000}")
    private long rotationGraceMs;

    private static final List<String> PUBLIC_AUTH_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout");

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ReactiveStringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 1. Extract access token
        String token = extractToken(exchange);
        if (!StringUtils.hasText(token)) {
            return onError(exchange, "Access token manquant", HttpStatus.UNAUTHORIZED);
        }

        // 2. Check Redis: was this token already rotated? (concurrent request)
        return redisTemplate.opsForValue()
                .get(ACCESS_ROTATED_PREFIX + token)
                .flatMap(cachedNewToken -> {
                    // Token was already rotated → reuse the cached new token
                    log.debug("Access token déjà rotationné (requête concurrente), utilisation du token caché");
                    Claims claims = jwtUtil.extractAllClaims(cachedNewToken);
                    return proceedWithToken(exchange, chain, cachedNewToken, claims);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Token not in Redis → first time seeing this token, validate + rotate
                    return validateAndRotate(exchange, chain, token);
                }));
    }

    /**
     * Validates the incoming JWT, generates a new rotated token, caches old→new
     * in Redis, and proceeds with the request.
     */
    private Mono<Void> validateAndRotate(ServerWebExchange exchange, GatewayFilterChain chain, String oldToken) {
        Claims claims;
        try {
            claims = jwtUtil.extractAllClaims(oldToken);
        } catch (ExpiredJwtException e) {
            return onError(exchange, "Access token expiré", HttpStatus.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException e) {
            return onError(exchange, "Access token invalide : " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        Object userIdObj = claims.get("userId");
        Object roleObj = claims.get("role");
        String email = claims.getSubject();

        if (userIdObj == null || roleObj == null) {
            return onError(exchange, "Claims obligatoires manquants (userId, role)", HttpStatus.UNAUTHORIZED);
        }

        String userId = String.valueOf(userIdObj);
        String role = String.valueOf(roleObj);

        // Generate rotated access token
        String newToken = jwtUtil.rotateAccessToken(userId, role, email);

        // Store old→new in Redis with grace-period TTL
        return redisTemplate.opsForValue()
                .set(ACCESS_ROTATED_PREFIX + oldToken, newToken, Duration.ofMillis(rotationGraceMs))
                .then(Mono.defer(() -> {
                    Claims newClaims = jwtUtil.extractAllClaims(newToken);
                    return proceedWithToken(exchange, chain, newToken, newClaims);
                }));
    }

    /**
     * Sets the rotated access token cookie and injects headers downstream.
     */
    private Mono<Void> proceedWithToken(ServerWebExchange exchange, GatewayFilterChain chain,
            String accessToken, Claims claims) {
        String userId = String.valueOf(claims.get("userId"));
        String role = String.valueOf(claims.get("role"));
        String email = claims.getSubject();

        log.debug("Authentification réussie pour userId={}, role={}", userId, role);

        // Set rotated access token cookie in response
        ResponseCookie rotatedCookie = ResponseCookie.from(accessTokenCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtUtil.getAccessTokenExpirationMs() / 1000)
                .sameSite(sameSite)
                .build();

        exchange.getResponse().addCookie(rotatedCookie);

        // Inject headers for downstream services
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Role", role)
                .header("X-User-Email", email != null ? email : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isPublicEndpoint(String path) {
        for (String publicEndpoint : PUBLIC_AUTH_ENDPOINTS) {
            if (path.equals(publicEndpoint) || path.startsWith(publicEndpoint + "/")) {
                return true;
            }
        }
        return path.startsWith("/actuator") || path.startsWith("/eureka");
    }

    private String extractToken(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(accessTokenCookieName);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value());

        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
