package ma.bawab.academic_service.controller;

import jakarta.validation.Valid;
import ma.bawab.academic_service.dto.*;
import ma.bawab.academic_service.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ParentService parentService;
    private final EtudiantService etudiantService;
    private final EtudiantCursusService cursusService;
    private final MatiereService matiereService;
    private final NoteService noteService;

    public AdminController(ParentService parentService,
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

    // ─── PARENTS ─────────────────────────────────────────────────────────────

    @GetMapping("/parents")
    public ResponseEntity<List<ParentDto>> getAllParents() {
        return ResponseEntity.ok(parentService.getAll());
    }

    @PostMapping("/parents")
    public ResponseEntity<ParentDto> createParent(
            @RequestParam Long userId,
            @Valid @RequestBody CreateParentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parentService.createProfil(userId, request));
    }

    @GetMapping("/parents/{id}")
    public ResponseEntity<ParentDto> getParent(@PathVariable Long id) {
        return ResponseEntity.ok(parentService.getProfilById(id));
    }

    @PutMapping("/parents/{id}")
    public ResponseEntity<ParentDto> updateParent(
            @PathVariable Long id,
            @Valid @RequestBody CreateParentRequest request) {
        return ResponseEntity.ok(parentService.updateProfil(id, request));
    }

    @DeleteMapping("/parents/{id}")
    public ResponseEntity<Void> deleteParent(@PathVariable Long id) {
        parentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── ÉTUDIANTS ───────────────────────────────────────────────────────────

    @GetMapping("/etudiants")
    public ResponseEntity<List<EtudiantDto>> getAllEtudiants() {
        return ResponseEntity.ok(etudiantService.getAll());
    }

    @PostMapping("/parents/{parentId}/etudiants")
    public ResponseEntity<EtudiantDto> createEtudiant(
            @PathVariable Long parentId,
            @Valid @RequestBody CreateEtudiantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etudiantService.createEtudiant(parentId, request));
    }

    @GetMapping("/etudiants/{id}")
    public ResponseEntity<EtudiantDto> getEtudiant(@PathVariable Long id) {
        return ResponseEntity.ok(etudiantService.getById(id));
    }

    @PutMapping("/etudiants/{id}")
    public ResponseEntity<EtudiantDto> updateEtudiant(
            @PathVariable Long id,
            @Valid @RequestBody CreateEtudiantRequest request) {
        return ResponseEntity.ok(etudiantService.update(id, request));
    }

    @DeleteMapping("/etudiants/{id}")
    public ResponseEntity<Void> deleteEtudiant(@PathVariable Long id) {
        etudiantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── CURSUS ──────────────────────────────────────────────────────────────

    @GetMapping("/etudiants/{id}/cursus")
    public ResponseEntity<List<EtudiantCursusDto>> getCursus(@PathVariable Long id) {
        return ResponseEntity.ok(cursusService.getCursus(id));
    }

    @PostMapping("/etudiants/{id}/cursus")
    public ResponseEntity<EtudiantCursusDto> addCursus(
            @PathVariable Long id,
            @Valid @RequestBody CreateCursusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursusService.addCursus(id, request));
    }

    @PutMapping("/cursus/{cursusId}/current")
    public ResponseEntity<EtudiantCursusDto> setCurrent(@PathVariable Long cursusId) {
        return ResponseEntity.ok(cursusService.setCurrent(cursusId));
    }

    // ─── MATIÈRES ────────────────────────────────────────────────────────────

    @GetMapping("/etudiants/{id}/matieres")
    public ResponseEntity<List<MatiereDto>> getMatieres(@PathVariable Long id) {
        return ResponseEntity.ok(matiereService.getMatieres(id));
    }

    @PostMapping("/etudiants/{id}/matieres")
    public ResponseEntity<MatiereDto> addMatiere(
            @PathVariable Long id,
            @Valid @RequestBody CreateMatiereRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matiereService.addMatiere(id, request));
    }

    @DeleteMapping("/matieres/{id}")
    public ResponseEntity<Void> deleteMatiere(@PathVariable Long id) {
        matiereService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── NOTES ───────────────────────────────────────────────────────────────

    @GetMapping("/matieres/{id}/notes")
    public ResponseEntity<NoteDto> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNote(id));
    }

    @PostMapping("/matieres/{id}/notes")
    public ResponseEntity<NoteDto> addNotes(
            @PathVariable Long id,
            @RequestBody UpdateNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.addNote(id, request));
    }

    @PutMapping("/notes/{id}")
    public ResponseEntity<NoteDto> updateNote(
            @PathVariable Long id,
            @RequestBody UpdateNoteRequest request) {
        return ResponseEntity.ok(noteService.updateNote(id, request));
    }
}
