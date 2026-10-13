package ma.bawab.academic_service.dto;

import java.util.List;

public class EtudiantDto {
    private Long id;
    private Long userId;
    private Long parentId;
    private String nom;
    private String prenom;
    private List<EtudiantCursusDto> cursus;
    private List<MatiereDto> matieres;

    public EtudiantDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public List<EtudiantCursusDto> getCursus() {
        return cursus;
    }

    public void setCursus(List<EtudiantCursusDto> cursus) {
        this.cursus = cursus;
    }

    public List<MatiereDto> getMatieres() {
        return matieres;
    }

    public void setMatieres(List<MatiereDto> matieres) {
        this.matieres = matieres;
    }
}
