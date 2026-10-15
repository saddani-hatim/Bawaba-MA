package ma.bawab.academic_service.service;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import ma.bawab.academic_service.dto.*;
import ma.bawab.academic_service.entity.*;
import ma.bawab.academic_service.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);
    private static final String BULKHEAD = "db-academic";

    private final NoteRepository noteRepository;
    private final MatiereRepository matiereRepository;

    public NoteService(NoteRepository noteRepository, MatiereRepository matiereRepository) {
        this.noteRepository = noteRepository;
        this.matiereRepository = matiereRepository;
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "addNoteFallback")
    @Transactional
    public NoteDto addNote(Long matiereId, UpdateNoteRequest request) {
        Matiere matiere = matiereRepository.findById(matiereId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matière introuvable"));
        if (noteRepository.findByMatiereId(matiereId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les notes existent déjà pour cette matière. Utilisez PUT pour modifier.");
        }
        Note note = new Note();
        note.setMatiere(matiere);
        if (request.getNote1() != null)
            note.setNote1(request.getNote1());
        if (request.getNote2() != null)
            note.setNote2(request.getNote2());
        if (request.getNote3() != null)
            note.setNote3(request.getNote3());
        return toDto(noteRepository.save(note));
    }

    public NoteDto addNoteFallback(Long matiereId, UpdateNoteRequest request, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur addNote : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "updateNoteFallback")
    @Transactional
    public NoteDto updateNote(Long noteId, UpdateNoteRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note introuvable"));
        if (request.getNote1() != null)
            note.setNote1(request.getNote1());
        if (request.getNote2() != null)
            note.setNote2(request.getNote2());
        if (request.getNote3() != null)
            note.setNote3(request.getNote3());
        return toDto(noteRepository.save(note));
    }

    public NoteDto updateNoteFallback(Long noteId, UpdateNoteRequest request, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur updateNote : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "getNoteFallback")
    @Transactional(readOnly = true)
    public NoteDto getNote(Long matiereId) {
        Note note = noteRepository.findByMatiereId(matiereId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune note pour cette matière"));
        return toDto(note);
    }

    public NoteDto getNoteFallback(Long matiereId, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur getNote : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    public static NoteDto toDto(Note n) {
        return new NoteDto(n.getId(), n.getNote1(), n.getNote2(), n.getNote3());
    }
}
