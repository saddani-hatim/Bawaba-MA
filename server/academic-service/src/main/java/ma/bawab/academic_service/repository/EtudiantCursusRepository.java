package ma.bawab.academic_service.repository;

import ma.bawab.academic_service.entity.EtudiantCursus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtudiantCursusRepository extends JpaRepository<EtudiantCursus, Long> {
    List<EtudiantCursus> findByEtudiantId(Long etudiantId);

    Optional<EtudiantCursus> findByEtudiantIdAndIsCurrentTrue(Long etudiantId);
}
