package ma.bawab.auth_service.service;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.bawab.auth_service.dto.*;
import ma.bawab.auth_service.entity.RefreshToken;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.repository.UserRepository;
import ma.bawab.auth_service.security.JwtService;
import ma.bawab.auth_service.security.RefreshTokenService;
import ma.bawab.auth_service.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service d'authentification protégé par un Bulkhead Resilience4j.
 *
 * <p>
 * Avec les virtual threads, des milliers de threads peuvent se créer
 * simultanément et saturer le pool HikariCP. Le bulkhead {@code db-auth}
 * limite la concurrence à 20 appels DB simultanés (=taille du pool).
 * Au-delà, la requête attend 500 ms puis reçoit un 503.
 */
@Service
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);
        private static final String BULKHEAD = "db-auth";

        private final UserRepository userRepository;
        private final RefreshTokenService refreshTokenService;
        private final JwtService jwtService;
        private final CookieUtil cookieUtil;
        private final PasswordEncoder passwordEncoder;

        public AuthService(UserRepository userRepository,
                        RefreshTokenService refreshTokenService,
                        JwtService jwtService,
                        CookieUtil cookieUtil,
                        PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.refreshTokenService = refreshTokenService;
                this.jwtService = jwtService;
                this.cookieUtil = cookieUtil;
                this.passwordEncoder = passwordEncoder;
        }

        // ── Register ─────────────────────────────────────────────────────────────

        @Bulkhead(name = BULKHEAD, fallbackMethod = "registerFallback")
        @Transactional
        public UserResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
                }

                String encodedPassword = passwordEncoder.encode(request.getPassword());
                User user = new User(request.getEmail(), encodedPassword, request.getRole());
                User savedUser = userRepository.save(user);

                return toUserResponse(savedUser);
        }

        public UserResponse registerFallback(RegisterRequest request, BulkheadFullException ex) {
                log.warn("Bulkhead db-auth saturé sur register : {}", ex.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Serveur surchargé, réessayez dans quelques secondes.");
        }

        // ── Login ─────────────────────────────────────────────────────────────────

        @Bulkhead(name = BULKHEAD, fallbackMethod = "loginFallback")
        @Transactional
        public AuthResponse login(LoginRequest request, HttpServletResponse response) {
                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                "Invalid email or password"));

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
                }

                // Generate RSA RS256 JWT access token
                String accessToken = jwtService.generateAccessToken(user);

                // Generate random hexadecimal refresh token and save to DB
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

                // Set HttpOnly cookies
                ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(accessToken,
                                jwtService.getAccessTokenExpirationMs());
                ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(refreshToken.getToken(),
                                refreshTokenService.getRefreshTokenExpirationMs());

                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                return new AuthResponse("Login successful", toUserResponse(user));
        }

        public AuthResponse loginFallback(LoginRequest request, HttpServletResponse response,
                        BulkheadFullException ex) {
                log.warn("Bulkhead db-auth saturé sur login : {}", ex.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Serveur surchargé, réessayez dans quelques secondes.");
        }

        // ── Refresh ───────────────────────────────────────────────────────────────

        @Bulkhead(name = BULKHEAD, fallbackMethod = "refreshFallback")
        @Transactional
        public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
                String tokenValue = cookieUtil.extractCookie(request, cookieUtil.getRefreshTokenCookieName())
                                .orElseThrow(
                                                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                                "Refresh token cookie is missing"));

                // Rotation + Redis grace-period for concurrent requests (handled internally)
                RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(tokenValue);

                // Generate new RSA RS256 access token
                String newAccessToken = jwtService.generateAccessToken(rotatedToken.getUser());

                // Update HttpOnly cookies
                ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(newAccessToken,
                                jwtService.getAccessTokenExpirationMs());
                ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(rotatedToken.getToken(),
                                refreshTokenService.getRefreshTokenExpirationMs());

                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                return new AuthResponse("Token refreshed successfully", toUserResponse(rotatedToken.getUser()));
        }

        public AuthResponse refreshFallback(HttpServletRequest request, HttpServletResponse response,
                        BulkheadFullException ex) {
                log.warn("Bulkhead db-auth saturé sur refresh : {}", ex.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Serveur surchargé, réessayez dans quelques secondes.");
        }

        // ── Logout ────────────────────────────────────────────────────────────────

        @Bulkhead(name = BULKHEAD, fallbackMethod = "logoutFallback")
        @Transactional
        public MessageResponse logout(HttpServletRequest request, HttpServletResponse response) {
                cookieUtil.extractCookie(request, cookieUtil.getRefreshTokenCookieName())
                                .ifPresent(refreshTokenService::deleteByToken);

                // Clear both HttpOnly cookies
                response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.createCleanAccessTokenCookie().toString());
                response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.createCleanRefreshTokenCookie().toString());

                return new MessageResponse("Logged out successfully");
        }

        public MessageResponse logoutFallback(HttpServletRequest request, HttpServletResponse response,
                        BulkheadFullException ex) {
                log.warn("Bulkhead db-auth saturé sur logout : {}", ex.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Serveur surchargé, réessayez dans quelques secondes.");
        }

        // ── Helpers ───────────────────────────────────────────────────────────────

        private UserResponse toUserResponse(User user) {
                return new UserResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getRole(),
                                user.getCreatedAt());
        }
}
