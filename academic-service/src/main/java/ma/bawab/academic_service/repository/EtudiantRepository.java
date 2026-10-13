package ma.bawab.academic_service.repository;

import ma.bawab.academic_service.entity.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    List<Etudiant> findByParentId(Long parentId);

    Optional<Etudiant> findByUserId(Long userId);
}
