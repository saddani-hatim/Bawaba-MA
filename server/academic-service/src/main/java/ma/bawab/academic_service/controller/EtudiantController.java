package ma.bawab.academic_service.controller;

import ma.bawab.academic_service.dto.*;
import ma.bawab.academic_service.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etudiant")
public class EtudiantController {

    private final EtudiantService etudiantService;
    private final NoteService noteService;

    public EtudiantController(EtudiantService etudiantService, NoteService noteService) {
        this.etudiantService = etudiantService;
        this.noteService = noteService;
    }

    // GET /api/etudiant/cursus — Voir son cursus (via X-User-Id injecté par API
    // Gateway)
    @GetMapping("/cursus")
    public ResponseEntity<List<EtudiantCursusDto>> getCursus(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(etudiantService.getCursusByUserId(userId));
    }

    // GET /api/etudiant/matieres — Voir ses matières
    @GetMapping("/matieres")
    public ResponseEntity<List<MatiereDto>> getMatieres(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(etudiantService.getMatieresByUserId(userId));
    }

    // GET /api/etudiant/matieres/{matiereId}/notes — Voir ses notes
    @GetMapping("/matieres/{matiereId}/notes")
    public ResponseEntity<NoteDto> getNotes(
            @PathVariable Long matiereId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(noteService.getNote(matiereId));
    }
}
