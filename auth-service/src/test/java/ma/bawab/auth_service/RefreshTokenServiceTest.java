package ma.bawab.auth_service;

import ma.bawab.auth_service.entity.RefreshToken;
import ma.bawab.auth_service.entity.Role;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.repository.RefreshTokenRepository;
import ma.bawab.auth_service.repository.UserRepository;
import ma.bawab.auth_service.security.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Verify random hex refresh token generation, DB persistence, and rotation")
    void testRefreshTokenLifecycle() {
        User user = new User("parent@bawab.ma", "encoded_pass", Role.parent);
        user = userRepository.save(user);

        // 1. Create refresh token
        RefreshToken created = refreshTokenService.createRefreshToken(user);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getToken()).matches("^[0-9a-fA-F]{64}$"); // 64 hex characters
        assertThat(created.getUser().getId()).isEqualTo(user.getId());
        assertThat(created.isExpired()).isFalse();

        // 2. Find by token in DB
        Optional<RefreshToken> found = refreshTokenService.findByToken(created.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("parent@bawab.ma");

        // 3. Rotate refresh token
        String oldHex = created.getToken();
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(created);
        assertThat(rotated.getToken()).isNotEqualTo(oldHex);
        assertThat(rotated.getToken()).matches("^[0-9a-fA-F]{64}$");

        // 4. Delete / revoke token
        refreshTokenService.deleteByToken(rotated.getToken());
        assertThat(refreshTokenRepository.findByToken(rotated.getToken())).isEmpty();
    }
}
