package ma.bawab.academic_service.service;

import ma.bawab.academic_service.dto.*;
import ma.bawab.academic_service.entity.*;
import ma.bawab.academic_service.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EtudiantService {

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

    @Transactional(readOnly = true)
    public EtudiantDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public EtudiantDto getByUserId(Long userId) {
        Etudiant etudiant = etudiantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        return toDto(etudiant);
    }

    @Transactional(readOnly = true)
    public List<EtudiantDto> getAll() {
        return etudiantRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EtudiantDto update(Long id, CreateEtudiantRequest request) {
        Etudiant etudiant = findOrThrow(id);
        etudiant.setNom(request.getNom());
        etudiant.setPrenom(request.getPrenom());
        return toDto(etudiantRepository.save(etudiant));
    }

    @Transactional
    public void delete(Long id) {
        if (!etudiantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable");
        }
        etudiantRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<MatiereDto> getMatieresByUserId(Long userId) {
        Etudiant etudiant = etudiantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        return matiereRepository.findByEtudiantId(etudiant.getId())
                .stream().map(this::toMatiereDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EtudiantCursusDto> getCursusByUserId(Long userId) {
        Etudiant etudiant = etudiantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        return cursusRepository.findByEtudiantId(etudiant.getId())
                .stream().map(this::toCursusDto).collect(Collectors.toList());
    }

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
