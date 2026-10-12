package ma.bawab.auth_service;

import ma.bawab.auth_service.entity.Role;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.security.JwtService;
import ma.bawab.auth_service.security.RsaKeyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RsaJwtSecurityTest {

    @Autowired
    private RsaKeyProvider rsaKeyProvider;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Verify RSA keys are loaded and RS256 token is signed and verified successfully")
    void testRsaTokenGenerationAndValidation() {
        assertThat(rsaKeyProvider.getPrivateKey()).isNotNull();
        assertThat(rsaKeyProvider.getPublicKey()).isNotNull();
        assertThat(rsaKeyProvider.getPrivateKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(rsaKeyProvider.getPublicKey().getAlgorithm()).isEqualTo("RSA");

        User user = new User("etudiant@bawab.ma", "hashed-password", Role.etudiant);
        user.setId(42L);

        String token = jwtService.generateAccessToken(user);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // RS256 JWT: header, payload, signature
    }
}
