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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service académique protégé par Resilience4j Bulkhead {@code db-academic}.
 *
 * <p>
 * Limite à 25 appels DB concurrents pour éviter de saturer HikariCP
 * quand les virtual threads multiplient les requêtes simultanément.
 */
@Service
public class EtudiantService {

    private static final Logger log = LoggerFactory.getLogger(EtudiantService.class);
    private static final String BULKHEAD = "db-academic";

    private final EtudiantRepository etudiantRepository;
    private final ParentRepository parentRepository;
    private final MatiereRepository matiereRepository;
    private final EtudiantCursusRepository cursusRepository;

    public EtudiantService(EtudiantRepository etudiantRepository,
            ParentRepository parentRepository,
            MatiereRepository matiereRepository,
            EtudiantCursusRepository cursusRepository) {
        this.etudiantRepository = etudiantRepository;
        this.parentRepository = parentRepository;
        this.matiereRepository = matiereRepository;
        this.cursusRepository = cursusRepository;
    }

    // ── Write operations ─────────────────────────────────────────────────────

    @Bulkhead(name = BULKHEAD, fallbackMethod = "createEtudiantFallback")
    @Transactional
    public EtudiantDto createEtudiant(Long parentId, CreateEtudiantRequest request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent introuvable"));
        Etudiant etudiant = new Etudiant();
        etudiant.setUserId(request.getUserId());
        etudiant.setNom(request.getNom());
        etudiant.setPrenom(request.getPrenom());
        etudiant.setParent(parent);
        return toDto(etudiantRepository.save(etudiant));
    }

    public EtudiantDto createEtudiantFallback(Long parentId, CreateEtudiantRequest request,
            BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur createEtudiant : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "updateFallback")
    @Transactional
    public EtudiantDto update(Long id, CreateEtudiantRequest request) {
        Etudiant etudiant = findOrThrow(id);
        etudiant.setNom(request.getNom());
        etudiant.setPrenom(request.getPrenom());
        return toDto(etudiantRepository.save(etudiant));
    }

    public EtudiantDto updateFallback(Long id, CreateEtudiantRequest request, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur update : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "deleteFallback")
    @Transactional
    public void delete(Long id) {
        if (!etudiantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable");
        }
        etudiantRepository.deleteById(id);
    }

    public void deleteFallback(Long id, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur delete : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    // ── Read operations ──────────────────────────────────────────────────────

    @Bulkhead(name = BULKHEAD, fallbackMethod = "getByIdFallback")
    @Transactional(readOnly = true)
    public EtudiantDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    public EtudiantDto getByIdFallback(Long id, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur getById : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "getByUserIdFallback")
    @Transactional(readOnly = true)
    public EtudiantDto getByUserId(Long userId) {
        Etudiant etudiant = etudiantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        return toDto(etudiant);
    }

    public EtudiantDto getByUserIdFallback(Long userId, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur getByUserId : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "getAllFallback")
    @Transactional(readOnly = true)
    public List<EtudiantDto> getAll() {
        return etudiantRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<EtudiantDto> getAllFallback(BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur getAll : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "getMatieresByUserIdFallback")
    @Transactional(readOnly = true)
    public List<MatiereDto> getMatieresByUserId(Long userId) {
        Etudiant etudiant = etudiantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        return matiereRepository.findByEtudiantId(etudiant.getId())
                .stream().map(this::toMatiereDto).collect(Collectors.toList());
    }

    public List<MatiereDto> getMatieresByUserIdFallback(Long userId, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur getMatieresByUserId : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    @Bulkhead(name = BULKHEAD, fallbackMethod = "getCursusByUserIdFallback")
    @Transactional(readOnly = true)
    public List<EtudiantCursusDto> getCursusByUserId(Long userId) {
        Etudiant etudiant = etudiantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        return cursusRepository.findByEtudiantId(etudiant.getId())
                .stream().map(this::toCursusDto).collect(Collectors.toList());
    }

    public List<EtudiantCursusDto> getCursusByUserIdFallback(Long userId, BulkheadFullException ex) {
        log.warn("Bulkhead db-academic saturé sur getCursusByUserId : {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Serveur surchargé, réessayez dans quelques secondes.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Etudiant findOrThrow(Long id) {
        return etudiantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
    }

    public EtudiantDto toDto(Etudiant e) {
        EtudiantDto dto = new EtudiantDto();
        dto.setId(e.getId());
        dto.setUserId(e.getUserId());
        dto.setParentId(e.getParent() != null ? e.getParent().getId() : null);
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setCursus(cursusRepository.findByEtudiantId(e.getId())
                .stream().map(this::toCursusDto).collect(Collectors.toList()));
        dto.setMatieres(matiereRepository.findByEtudiantId(e.getId())
                .stream().map(this::toMatiereDto).collect(Collectors.toList()));
        return dto;
    }

    private EtudiantCursusDto toCursusDto(EtudiantCursus c) {
        return new EtudiantCursusDto(c.getId(), c.getAnneeScolaire(), c.getNiveau(), c.isCurrent());
    }

    private MatiereDto toMatiereDto(Matiere m) {
        NoteDto noteDto = null;
        if (m.getNote() != null) {
            Note n = m.getNote();
            noteDto = new NoteDto(n.getId(), n.getNote1(), n.getNote2(), n.getNote3());
        }
        return new MatiereDto(m.getId(), m.getNomMatiere(), noteDto);
    }
}
