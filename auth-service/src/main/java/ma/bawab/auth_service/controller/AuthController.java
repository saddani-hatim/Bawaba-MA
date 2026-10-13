package ma.bawab.auth_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ma.bawab.auth_service.dto.*;
import ma.bawab.auth_service.service.AuthService;
import ma.bawab.auth_service.service.MeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MeService meService;

    public AuthController(AuthService authService, MeService meService) {
        this.authService = authService;
        this.meService = meService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshToken(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request,
            HttpServletResponse response) {
        MessageResponse messageResponse = authService.logout(request, response);
        return ResponseEntity.ok(messageResponse);
    }

    /**
     * GET /api/auth/me
     *
     * Headers injectés par l'API Gateway après vérification du JWT :
     * X-User-Id : id de l'utilisateur authentifié
     * X-Role : rôle de l'utilisateur (admin | parent | etudiant)
     *
     * Retourne le profil enrichi selon le rôle (JOIN parents ou etudiants),
     * ou les infos basiques si aucun profil complémentaire n'existe encore.
     * Ne retourne jamais le mot de passe.
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role") String role) {
        MeResponse profile = meService.getMe(userId, role);
        return ResponseEntity.ok(profile);
    }
}
