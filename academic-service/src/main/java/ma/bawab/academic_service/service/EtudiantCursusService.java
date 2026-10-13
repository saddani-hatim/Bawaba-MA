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
public class EtudiantCursusService {

    private final EtudiantCursusRepository cursusRepository;
    private final EtudiantRepository etudiantRepository;

    public EtudiantCursusService(EtudiantCursusRepository cursusRepository,
            EtudiantRepository etudiantRepository) {
        this.cursusRepository = cursusRepository;
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional
    public EtudiantCursusDto addCursus(Long etudiantId, CreateCursusRequest request) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Étudiant introuvable"));
        EtudiantCursus cursus = new EtudiantCursus();
        cursus.setEtudiant(etudiant);
        cursus.setAnneeScolaire(request.getAnneeScolaire());
        cursus.setNiveau(request.getNiveau());
        cursus.setCurrent(true);
        return toDto(cursusRepository.save(cursus));
    }

    @Transactional(readOnly = true)
    public List<EtudiantCursusDto> getCursus(Long etudiantId) {
        return cursusRepository.findByEtudiantId(etudiantId)
                .stream().map(EtudiantCursusService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EtudiantCursusDto setCurrent(Long cursusId) {
        EtudiantCursus cursus = cursusRepository.findById(cursusId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cursus introuvable"));
        // Désactiver tous les autres cursus de cet étudiant
        cursusRepository.findByEtudiantId(cursus.getEtudiant().getId()).forEach(c -> {
            c.setCurrent(false);
            cursusRepository.save(c);
        });
        cursus.setCurrent(true);
        return toDto(cursusRepository.save(cursus));
    }

    public static EtudiantCursusDto toDto(EtudiantCursus c) {
        return new EtudiantCursusDto(c.getId(), c.getAnneeScolaire(), c.getNiveau(), c.isCurrent());
    }
}
