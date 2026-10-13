package ma.bawab.academic_service.repository;

import ma.bawab.academic_service.entity.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatiereRepository extends JpaRepository<Matiere, Long> {
    List<Matiere> findByEtudiantId(Long etudiantId);
}
