package ma.bawab.academic_service.repository;

import ma.bawab.academic_service.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    Optional<Note> findByMatiereId(Long matiereId);
}
