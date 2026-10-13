package ma.bawab.auth_service.repository;

import ma.bawab.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * JOIN users ⟵ parents (pour le rôle parent)
     * Colonnes retournées : id, email, role, created_at, nom, prenom, telephone
     */
    @Query(value = """
            SELECT u.id, u.email, u.role, u.created_at,
                   p.nom, p.prenom, p.telephone
            FROM users u
            JOIN parents p ON p.user_id = u.id
            WHERE u.id = :userId
            """, nativeQuery = true)
    Optional<Object[]> findParentMe(@Param("userId") Long userId);

    /**
     * JOIN users ⟵ etudiants (pour le rôle etudiant)
     * Colonnes retournées : id, email, role, created_at, nom, prenom
     */
    @Query(value = """
            SELECT u.id, u.email, u.role, u.created_at,
                   e.nom, e.prenom
            FROM users u
            JOIN etudiants e ON e.user_id = u.id
            WHERE u.id = :userId
            """, nativeQuery = true)
    Optional<Object[]> findEtudiantMe(@Param("userId") Long userId);
}
