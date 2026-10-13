package ma.bawab.academic_service.controller;
import ma.bawab.academic_service.dto.*;
import ma.bawab.academic_service.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parent")
public class ParentController {

    private final ParentService parentService;
    private final EtudiantService etudiantService;
    private final EtudiantCursusService cursusService;
    private final MatiereService matiereService;
    private final NoteService noteService;

    public ParentController(ParentService parentService,
            EtudiantService etudiantService,
            EtudiantCursusService cursusService,
            MatiereService matiereService,
            NoteService noteService) {
        this.parentService = parentService;
        this.etudiantService = etudiantService;
        this.cursusService = cursusService;
        this.matiereService = matiereService;
        this.noteService = noteService;
    }

    // GET /api/parent/etudiants — Voir ses enfants (via X-User-Id injecté par API
    // Gateway)
    @GetMapping("/etudiants")
    public ResponseEntity<List<EtudiantDto>> getEtudiants(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(parentService.getEtudiantsByUserId(userId));
    }

    // GET /api/parent/etudiants/{etudiantId} — Détail enfant
    @GetMapping("/etudiants/{etudiantId}")
    public ResponseEntity<EtudiantDto> getEtudiant(
            @PathVariable Long etudiantId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(etudiantService.getById(etudiantId));
    }

    // GET /api/parent/etudiants/{etudiantId}/cursus — Voir cursus enfant
    @GetMapping("/etudiants/{etudiantId}/cursus")
    public ResponseEntity<List<EtudiantCursusDto>> getCursus(
            @PathVariable Long etudiantId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cursusService.getCursus(etudiantId));
    }

    // GET /api/parent/etudiants/{etudiantId}/matieres — Voir matières enfant
    @GetMapping("/etudiants/{etudiantId}/matieres")
    public ResponseEntity<List<MatiereDto>> getMatieres(
            @PathVariable Long etudiantId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(matiereService.getMatieres(etudiantId));
    }

    // GET /api/parent/matieres/{matiereId}/notes — Voir notes
    @GetMapping("/matieres/{matiereId}/notes")
    public ResponseEntity<NoteDto> getNotes(
            @PathVariable Long matiereId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(noteService.getNote(matiereId));
    }
}
