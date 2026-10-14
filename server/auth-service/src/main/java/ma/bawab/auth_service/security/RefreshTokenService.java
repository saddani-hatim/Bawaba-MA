package ma.bawab.auth_service.security;

import ma.bawab.auth_service.entity.RefreshToken;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages refresh token lifecycle with rotation + Redis-based grace period.
 *
 * Rotation flow:
 *  1. Token found in DB → rotate normally → store oldToken→newToken in Redis with TTL.
 *  2. Token NOT found in DB → check Redis:
 *     - Found in Redis → concurrent request, return the already-rotated token from DB.
 *     - Not in Redis   → token is invalid / expired grace period → 401.
 */
@Service
public class RefreshTokenService {

    private static final String REDIS_PREFIX = "refresh:used:";

    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /** Grace period for concurrent /refresh requests (ms). Default = 30 s. */
    @Value("${jwt.refresh-token-grace-period-ms:30000}")
    private long gracePeriodMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               StringRedisTemplate redisTemplate) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisTemplate = redisTemplate;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public String generateRandomHexToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String tokenValue = generateRandomHexToken(32); // 64 hex chars
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(refreshTokenExpirationMs * 1_000_000);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    // ── Rotate with Redis grace period ───────────────────────────────────────

    /**
     * Rotates the refresh token identified by {@code oldTokenValue}.
     *
     * <p>If the token is not found in DB, checks Redis to handle concurrent
     * requests that arrive with the same old token within the grace period.
     *
     * @return the new (active) {@link RefreshToken}
     * @throws ResponseStatusException 401 if the token is invalid or grace period expired
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        Optional<RefreshToken> dbToken = refreshTokenRepository.findByToken(oldTokenValue);

        if (dbToken.isPresent()) {
            RefreshToken existing = dbToken.get();

            // Check expiration
            if (existing.isExpired()) {
                refreshTokenRepository.delete(existing);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Refresh token expired. Please sign in again.");
            }

            // Create new token record
            String newTokenValue = generateRandomHexToken(32);
            LocalDateTime newExpiresAt = LocalDateTime.now()
                    .plusNanos(refreshTokenExpirationMs * 1_000_000);

            RefreshToken newToken = new RefreshToken(existing.getUser(), newTokenValue, newExpiresAt);
            newToken = refreshTokenRepository.save(newToken);

            // Delete old token from DB
            refreshTokenRepository.delete(existing);

            // Store old→new mapping in Redis with grace-period TTL
            redisTemplate.opsForValue().set(
                    REDIS_PREFIX + oldTokenValue,
                    newTokenValue,
                    gracePeriodMs,
                    TimeUnit.MILLISECONDS
            );

            return newToken;
        }

        // Token not in DB → check Redis grace period (concurrent request scenario)
        String newTokenValue = redisTemplate.opsForValue().get(REDIS_PREFIX + oldTokenValue);
        if (newTokenValue != null) {
            // Return the already-rotated token
            return refreshTokenRepository.findByToken(newTokenValue)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Rotated token not found. Please sign in again."));
        }

        // Not in Redis either → truly invalid token
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.");
    }

    // ── Find / Delete ────────────────────────────────────────────────────────

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
