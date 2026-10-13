package ma.bawab.auth_service.service;

import ma.bawab.auth_service.dto.EtudiantMeResponse;
import ma.bawab.auth_service.dto.MeResponse;
import ma.bawab.auth_service.dto.ParentMeResponse;
import ma.bawab.auth_service.entity.Role;
import ma.bawab.auth_service.entity.User;
import ma.bawab.auth_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MeService {

    private final UserRepository userRepository;

    public MeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId, String roleHeader) {

        Role role;
        try {
            role = Role.valueOf(roleHeader);
        } catch (IllegalArgumentException e) {
            role = null;
        }

        if (Role.parent.equals(role)) {
            Optional<Object[]> row = userRepository.findParentMe(userId);
            if (row.isPresent()) {
                Object[] r = row.get();
                return new ParentMeResponse(
                        toLong(r[0]),
                        (String) r[1],
                        Role.valueOf((String) r[2]),
                        toLocalDateTime(r[3]),
                        (String) r[4],
                        (String) r[5],
                        (String) r[6]);
            }
            // Pas de ligne dans parents → fallback infos basiques
            return toMeResponse(findUserOrThrow(userId));
        }

        if (Role.etudiant.equals(role)) {
            Optional<Object[]> row = userRepository.findEtudiantMe(userId);
            if (row.isPresent()) {
                Object[] r = row.get();
                return new EtudiantMeResponse(
                        toLong(r[0]),
                        (String) r[1],
                        Role.valueOf((String) r[2]),
                        toLocalDateTime(r[3]),
                        (String) r[4],
                        (String) r[5]);
            }
            // Pas de ligne dans etudiants → fallback infos basiques
            return toMeResponse(findUserOrThrow(userId));
        }

        // admin ou rôle non géré → infos basiques uniquement
        return toMeResponse(findUserOrThrow(userId));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    /**
     * JDBC retourne les ids en Long, Integer ou BigInteger selon le driver.
     */
    private Long toLong(Object o) {
        if (o instanceof Long l)
            return l;
        if (o instanceof Integer i)
            return i.longValue();
        if (o instanceof BigInteger bi)
            return bi.longValue();
        if (o instanceof Number n)
            return n.longValue();
        throw new IllegalArgumentException("Impossible de convertir en Long : " + o);
    }

    /**
     * H2 / MySQL retournent created_at sous forme Timestamp ou LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(Object o) {
        if (o instanceof LocalDateTime ldt)
            return ldt;
        if (o instanceof Timestamp ts)
            return ts.toLocalDateTime();
        throw new IllegalArgumentException("Impossible de convertir en LocalDateTime : " + o);
    }
}
