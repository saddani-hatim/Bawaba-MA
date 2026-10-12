package ma.bawab.auth_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import ma.bawab.auth_service.dto.LoginRequest;
import ma.bawab.auth_service.dto.RegisterRequest;
import ma.bawab.auth_service.entity.Role;
import ma.bawab.auth_service.repository.RefreshTokenRepository;
import ma.bawab.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerIntegrationTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @BeforeEach
        void setUp() {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        @DisplayName("Complete authentication flow: Register, Login (HttpOnly Cookies + RSA JWT + DB Hex Refresh Token), Refresh Token, and Logout")
        void testCompleteAuthenticationFlow() throws Exception {
                // 1. REGISTER
                RegisterRequest registerRequest = new RegisterRequest("admin@bawab.ma", "secret123", Role.admin);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("admin@bawab.ma"))
                                .andExpect(jsonPath("$.role").value("admin"));

                // Register duplicate email fails with 400
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isBadRequest());

                // 2. LOGIN
                LoginRequest loginRequest = new LoginRequest("admin@bawab.ma", "secret123");

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Login successful"))
                                .andExpect(jsonPath("$.user.email").value("admin@bawab.ma"))
                                .andExpect(jsonPath("$.user.role").value("admin"))
                                .andReturn();

                // Verify Set-Cookie headers
                var setCookies = loginResult.getResponse().getHeaders("Set-Cookie");
                assertThat(setCookies).isNotEmpty();

                Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
                Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

                assertThat(accessCookie).isNotNull();
                assertThat(accessCookie.isHttpOnly()).isTrue();
                assertThat(accessCookie.getPath()).isEqualTo("/");

                assertThat(refreshCookie).isNotNull();
                assertThat(refreshCookie.isHttpOnly()).isTrue();
                assertThat(refreshCookie.getPath()).isEqualTo("/api/auth");

                // Verify Access Token is present and is a valid 3-part JWT
                String jwtAccessToken = accessCookie.getValue();
                assertThat(jwtAccessToken).isNotBlank();
                assertThat(jwtAccessToken.split("\\.")).hasSize(3);

                // Verify Refresh Token is random hexadecimal and saved in DB
                String hexRefreshToken = refreshCookie.getValue();
                assertThat(hexRefreshToken).matches("^[0-9a-fA-F]{64}$");
                assertThat(refreshTokenRepository.findByToken(hexRefreshToken)).isPresent();

                // 3. REFRESH TOKEN
                MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                                .cookie(refreshCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                                .andExpect(jsonPath("$.user.email").value("admin@bawab.ma"))
                                .andReturn();

                Cookie newAccessCookie = refreshResult.getResponse().getCookie("access_token");
                Cookie newRefreshCookie = refreshResult.getResponse().getCookie("refresh_token");

                assertThat(newAccessCookie).isNotNull();
                assertThat(newRefreshCookie).isNotNull();
                assertThat(newAccessCookie.getValue()).isNotBlank();
                assertThat(newAccessCookie.getValue().split("\\.")).hasSize(3);

                // Old refresh token was rotated to the new one
                assertThat(refreshTokenRepository.findByToken(newRefreshCookie.getValue())).isPresent();

                // 4. LOGOUT
                MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                                .cookie(newRefreshCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                                .andReturn();

                // Both cookies cleared with maxAge 0
                Cookie clearedAccessCookie = logoutResult.getResponse().getCookie("access_token");
                Cookie clearedRefreshCookie = logoutResult.getResponse().getCookie("refresh_token");

                assertThat(clearedAccessCookie.getMaxAge()).isEqualTo(0);
                assertThat(clearedRefreshCookie.getMaxAge()).isEqualTo(0);

                // Refresh token removed from DB
                assertThat(refreshTokenRepository.findByToken(newRefreshCookie.getValue())).isEmpty();
        }
}
