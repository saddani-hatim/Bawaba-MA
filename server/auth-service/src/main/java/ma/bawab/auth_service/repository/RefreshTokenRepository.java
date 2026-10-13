package ma.bawab.auth_service.repository;

import ma.bawab.auth_service.entity.RefreshToken;
import ma.bawab.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    void deleteByToken(String token);

    @Modifying
    void deleteByUser(User user);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
