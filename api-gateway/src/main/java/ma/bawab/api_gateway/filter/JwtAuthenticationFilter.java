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
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessTokenCookieName;

    /**
     * Endpoints publics de l'auth-service ne nécessitant pas de token d'accès.
     * Note : /api/auth/me N'EST PAS dans cette liste car il exige la vérification du token.
     */
    private static final List<String> PUBLIC_AUTH_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. Laisser passer les requêtes CORS preflight (OPTIONS)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 2. Laisser passer les endpoints publics
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 3. Extraction de l'access token depuis le cookie HttpOnly (ou Authorization header)
        String token = extractToken(exchange);
        if (!StringUtils.hasText(token)) {
            log.warn("Tentative d'accès non autorisée à '{}' : cookie HttpOnly '{}' manquant", path, accessTokenCookieName);
            return onError(exchange, "Access token manquant dans le cookie HttpOnly (" + accessTokenCookieName + ")", HttpStatus.UNAUTHORIZED);
        }

        // 4. Validation du token JWT RS256 et extraction des claims
        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            Object userIdObj = claims.get("userId");
            Object roleObj = claims.get("role");
            String email = claims.getSubject();

            if (userIdObj == null || roleObj == null) {
                log.warn("Token JWT incomplet pour '{}' : userId ou role manquant", path);
                return onError(exchange, "Claims obligatoires manquants (userId, role)", HttpStatus.UNAUTHORIZED);
            }

            String userId = String.valueOf(userIdObj);
            String role = String.valueOf(roleObj);

            log.debug("Authentification réussie pour userId={}, role={}, path={}", userId, role, path);

            // 5. Injection des headers X-User-Id et X-Role vers le microservice aval
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Role", role)
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré pour '{}' : {}", path, e.getMessage());
            return onError(exchange, "Access token expiré", HttpStatus.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT invalide pour '{}' : {}", path, e.getMessage());
            return onError(exchange, "Access token invalide : " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        for (String publicEndpoint : PUBLIC_AUTH_ENDPOINTS) {
            if (path.equals(publicEndpoint) || path.startsWith(publicEndpoint + "/")) {
                return true;
            }
        }
        return path.startsWith("/actuator") || path.startsWith("/eureka");
    }

    private String extractToken(ServerWebExchange exchange) {
        // Priorité 1 : Cookie HttpOnly
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(accessTokenCookieName);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }

        // Priorité 2 (fallback) : Header Authorization Bearer
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
                exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Exécuter avant le routage mais après les filtres CORS
        return -100;
    }
}
