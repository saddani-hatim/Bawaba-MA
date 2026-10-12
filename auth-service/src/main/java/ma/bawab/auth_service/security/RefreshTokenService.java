package ma.bawab.auth_service.security;

import ma.bawab.auth_service.entity.RefreshToken;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateRandomHexToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String tokenValue = generateRandomHexToken(32); // 64 hex characters
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token was expired. Please make a new signin request.");
        }
        return token;
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken existingToken) {
        verifyExpiration(existingToken);
        String newTokenValue = generateRandomHexToken(32);
        LocalDateTime newExpiresAt = LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000);

        existingToken.setToken(newTokenValue);
        existingToken.setExpiresAt(newExpiresAt);
        return refreshTokenRepository.save(existingToken);
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
