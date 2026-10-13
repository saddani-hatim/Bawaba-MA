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
public class ParentService {

    private final ParentRepository parentRepository;
    private final EtudiantRepository etudiantRepository;

    public ParentService(ParentRepository parentRepository, EtudiantRepository etudiantRepository) {
        this.parentRepository = parentRepository;
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional
    public ParentDto createProfil(Long userId, CreateParentRequest request) {
        if (parentRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un profil parent existe déjà pour cet utilisateur");
        }
        Parent parent = new Parent(userId, request.getNom(), request.getPrenom(), request.getTelephone());
        return toDto(parentRepository.save(parent));
    }

    @Transactional(readOnly = true)
    public ParentDto getProfilByUserId(Long userId) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil parent introuvable"));
        return toDto(parent);
    }

    @Transactional(readOnly = true)
    public ParentDto getProfilById(Long id) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil parent introuvable"));
        return toDto(parent);
    }

    @Transactional
    public ParentDto updateProfil(Long id, CreateParentRequest request) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil parent introuvable"));
        parent.setNom(request.getNom());
        parent.setPrenom(request.getPrenom());
        parent.setTelephone(request.getTelephone());
        return toDto(parentRepository.save(parent));
    }

    @Transactional(readOnly = true)
    public List<ParentDto> getAll() {
        return parentRepository.findAll().stream().map(ParentService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!parentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil parent introuvable");
        }
        parentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EtudiantDto> getEtudiantsByUserId(Long userId) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil parent introuvable"));
        return etudiantRepository.findByParentId(parent.getId())
                .stream().map(this::toEtudiantDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EtudiantDto> getEtudiantsByParentId(Long parentId) {
        return etudiantRepository.findByParentId(parentId)
                .stream().map(this::toEtudiantDto).collect(Collectors.toList());
    }

    public static ParentDto toDto(Parent p) {
        return new ParentDto(p.getId(), p.getUserId(), p.getNom(), p.getPrenom(), p.getTelephone());
    }

    private EtudiantDto toEtudiantDto(Etudiant e) {
        EtudiantDto dto = new EtudiantDto();
        dto.setId(e.getId());
        dto.setUserId(e.getUserId());
        dto.setParentId(e.getParent() != null ? e.getParent().getId() : null);
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        return dto;
    }
}
