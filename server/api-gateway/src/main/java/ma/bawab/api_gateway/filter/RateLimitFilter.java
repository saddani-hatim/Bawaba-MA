package ma.bawab.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GlobalFilter de rate limiting basé sur Redis (Fixed Window).
 *
 * <p>Deux paliers selon le type d'endpoint :
 * <ul>
 *   <li><b>Auth strict</b> (/login, /register) : 5 req / 60 s — anti brute-force</li>
 *   <li><b>API général</b> : 60 req / 60 s — usage normal </li>
 * </ul>
 *
 * <p>Clé Redis :  {@code ratelimit:<tier>:<IP>}
 * Algorithme : INCR + EXPIRE (window fixe). L'EXPIRE n'est posée qu'au
 * premier incrément (valeur == 1) pour éviter de repousser la fenêtre.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Endpoints d'authentification à limiter strictement. */
    private static final List<String> STRICT_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    /** Limite d'appels pour les endpoints AUTH sensibles (par minute). */
    private static final int STRICT_LIMIT = 5;

    /** Limite d'appels pour l'API générale (par minute). */
    private static final int GENERAL_LIMIT = 60;

    /** Durée de la fenêtre (1 minute). */
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ── Filter ──────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // Toujours laisser passer les preflight CORS
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String ip  = resolveClientIp(exchange);
        String path = exchange.getRequest().getPath().value();
        boolean strict = isStrictEndpoint(path);

        String redisKey = strict
                ? "ratelimit:auth:" + ip
                : "ratelimit:api:" + ip;
        int limit = strict ? STRICT_LIMIT : GENERAL_LIMIT;

        return checkRateLimit(exchange, chain, redisKey, limit, strict);
    }

    // ── Core logic ──────────────────────────────────────────────────────────

    private Mono<Void> checkRateLimit(ServerWebExchange exchange,
                                      GatewayFilterChain chain,
                                      String redisKey,
                                      int limit,
                                      boolean strict) {
        return redisTemplate.opsForValue()
                .increment(redisKey)                          // INCR (atomique)
                .flatMap(count -> {
                    if (count == 1) {
                        // Première requête de la fenêtre → poser l'expiration
                        return redisTemplate.expire(redisKey, WINDOW)
                                .then(proceed(exchange, chain, count, limit, strict));
                    }
                    return proceed(exchange, chain, count, limit, strict);
                });
    }

    private Mono<Void> proceed(ServerWebExchange exchange,
                               GatewayFilterChain chain,
                               long count,
                               int limit,
                               boolean strict) {
        if (count > limit) {
            String tier = strict ? "auth" : "general";
            log.warn("Rate limit dépassé [{}] pour IP={} ({}/{})",
                    tier,
                    resolveClientIp(exchange),
                    count,
                    limit);
            return onRateLimitExceeded(exchange, limit, strict);
        }
        return chain.filter(exchange);
    }

    // ── Response 429 ────────────────────────────────────────────────────────

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, int limit, boolean strict) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Indique au client combien de temps attendre (max 60 s)
        response.getHeaders().set("Retry-After", "60");

        String message = strict
                ? "Trop de tentatives de connexion. Réessayez dans 1 minute."
                : "Trop de requêtes. Limite : " + limit + " requêtes par minute.";

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(),
                message,
                exchange.getRequest().getPath().value());

        DataBuffer buffer = response.bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Résout l'IP réelle du client en tenant compte du proxy/load-balancer.
     * Priorité : X-Forwarded-For → X-Real-IP → Remote Address.
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For peut contenir plusieurs IPs séparées par des virgules
            return forwarded.split(",")[0].trim();
        }
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        var address = exchange.getRequest().getRemoteAddress();
        return address != null ? address.getAddress().getHostAddress() : "unknown";
    }

    private boolean isStrictEndpoint(String path) {
        for (String endpoint : STRICT_ENDPOINTS) {
            if (path.equals(endpoint) || path.startsWith(endpoint + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ordre -200 → s'exécute AVANT le JwtAuthenticationFilter (-100).
     * Le rate limit est appliqué même avant la validation du token.
     */
    @Override
    public int getOrder() {
        return -200;
    }
}
