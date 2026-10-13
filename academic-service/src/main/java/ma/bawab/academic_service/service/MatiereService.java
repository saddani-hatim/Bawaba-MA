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
public class MatiereService {

    private final MatiereRepository matiereRepository;
    private final EtudiantRepository etudiantRepository;

    public MatiereService(MatiereRepository matiereRepository, EtudiantRepository etudiantRepository) {
        this.matiereRepository = matiereRepository;
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional
    public MatiereDto addMatiere(Long etudiantId, CreateMatiereRequest request) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        Matiere matiere = new Matiere();
        matiere.setEtudiant(etudiant);
        matiere.setNomMatiere(request.getNomMatiere());
        return toDto(matiereRepository.save(matiere));
    }

    @Transactional(readOnly = true)
    public List<MatiereDto> getMatieres(Long etudiantId) {
        return matiereRepository.findByEtudiantId(etudiantId)
                .stream().map(MatiereService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!matiereRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matière introuvable");
        }
        matiereRepository.deleteById(id);
    }

    public static MatiereDto toDto(Matiere m) {
        NoteDto noteDto = null;
        if (m.getNote() != null) {
            Note n = m.getNote();
            noteDto = new NoteDto(n.getId(), n.getNote1(), n.getNote2(), n.getNote3());
        }
        return new MatiereDto(m.getId(), m.getNomMatiere(), noteDto);
    }
}
