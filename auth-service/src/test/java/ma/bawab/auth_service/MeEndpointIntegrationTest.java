package ma.bawab.auth_service;

import ma.bawab.auth_service.entity.Role;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.repository.RefreshTokenRepository;
import ma.bawab.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour GET /api/auth/me
 *
 * L'endpoint lit les headers X-User-Id et X-Role injectés par la Gateway.
 * Les tests vérifient :
 * 1. admin → retourne infos basiques (pas de password)
 * 2. parent avec ligne dans parents → retourne nom, prenom, telephone
 * 3. etudiant avec ligne dans etudiants → retourne nom, prenom
 * 4. parent sans ligne dans parents → fallback infos basiques
 * 5. userId inexistant → 404
 * 6. headers manquants → 400
 */
@SpringBootTest
class MeEndpointIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Nettoyage dans l'ordre FK (tokens → users)
        jdbcTemplate.execute("DELETE FROM etudiants");
        jdbcTemplate.execute("DELETE FROM parents");
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Création des tables auxiliaires si elles n'existent pas encore
        // (H2 MODE=MySQL, compatible avec la DDL MySQL)
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parents (
                    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id    BIGINT NOT NULL UNIQUE,
                    nom        VARCHAR(100) NOT NULL,
                    prenom     VARCHAR(100) NOT NULL,
                    telephone  VARCHAR(20)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS etudiants (
                    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id   BIGINT UNIQUE,
                    parent_id BIGINT,
                    nom       VARCHAR(100) NOT NULL,
                    prenom    VARCHAR(100) NOT NULL
                )
                """);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 1. Admin → infos basiques uniquement
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me admin → retourne id, email, role, createdAt (pas de password)")
    void testGetMe_admin() throws Exception {
        User admin = registerUser("admin@bawab.ma", Role.admin);

        mockMvc.perform(get("/api/auth/me")
                .header("X-User-Id", admin.getId())
                .header("X-Role", "admin")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.email").value("admin@bawab.ma"))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.createdAt").exists())
                // Le mot de passe NE DOIT JAMAIS apparaître
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2. Parent avec profil → JOIN retourne nom, prenom, telephone
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me parent avec profil → JOIN parents retourne nom, prenom, telephone")
    void testGetMe_parentWithProfile() throws Exception {
        User parent = registerUser("parent@bawab.ma", Role.parent);

        jdbcTemplate.update(
                "INSERT INTO parents (user_id, nom, prenom, telephone) VALUES (?, ?, ?, ?)",
                parent.getId(), "Alami", "Hassan", "0612345678");

        mockMvc.perform(get("/api/auth/me")
                .header("X-User-Id", parent.getId())
                .header("X-Role", "parent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parent@bawab.ma"))
                .andExpect(jsonPath("$.role").value("parent"))
                .andExpect(jsonPath("$.nom").value("Alami"))
                .andExpect(jsonPath("$.prenom").value("Hassan"))
                .andExpect(jsonPath("$.telephone").value("0612345678"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 3. Etudiant avec profil → JOIN retourne nom, prenom
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me etudiant avec profil → JOIN etudiants retourne nom, prenom")
    void testGetMe_etudiantWithProfile() throws Exception {
        User etudiant = registerUser("etudiant@bawab.ma", Role.etudiant);

        jdbcTemplate.update(
                "INSERT INTO etudiants (user_id, nom, prenom) VALUES (?, ?, ?)",
                etudiant.getId(), "Ezzahraoui", "Fatima");

        mockMvc.perform(get("/api/auth/me")
                .header("X-User-Id", etudiant.getId())
                .header("X-Role", "etudiant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("etudiant@bawab.ma"))
                .andExpect(jsonPath("$.role").value("etudiant"))
                .andExpect(jsonPath("$.nom").value("Ezzahraoui"))
                .andExpect(jsonPath("$.prenom").value("Fatima"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 4. Parent sans profil dans parents → fallback infos basiques
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me parent sans ligne dans parents → fallback MeResponse basique")
    void testGetMe_parentWithoutProfile_fallback() throws Exception {
        User parent = registerUser("parent2@bawab.ma", Role.parent);

        // Aucune insertion dans parents

        mockMvc.perform(get("/api/auth/me")
                .header("X-User-Id", parent.getId())
                .header("X-Role", "parent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parent2@bawab.ma"))
                .andExpect(jsonPath("$.role").value("parent"))
                // Pas de nom/prenom en fallback
                .andExpect(jsonPath("$.nom").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 5. userId inexistant → 404
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me avec userId inexistant → 404")
    void testGetMe_userNotFound() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("X-User-Id", 99999L)
                .header("X-Role", "admin"))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 6. Headers manquants → 400
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me sans headers X-User-Id et X-Role → 400")
    void testGetMe_missingHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────────────────
    // helper
    // ──────────────────────────────────────────────────────────────────────

    private User registerUser(String email, Role role) throws Exception {
        mockMvc.perform(get("/api/auth/register") // utilisé juste pour créer via repo
                .contentType(MediaType.APPLICATION_JSON));
        // Création directe via repository (plus rapide que passer par HTTP register)
        User user = new User(email, "$2a$10$dummyHashForTest", role);
        return userRepository.save(user);
    }
}
