package ma.bawab.auth_service.dto;

import ma.bawab.auth_service.entity.Role;
import java.time.LocalDateTime;

public class EtudiantMeResponse extends MeResponse {

    private String nom;
    private String prenom;

    public EtudiantMeResponse() {
    }

    public EtudiantMeResponse(Long id, String email, Role role, LocalDateTime createdAt,
            String nom, String prenom) {
        super(id, email, role, createdAt);
        this.nom = nom;
        this.prenom = prenom;
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
}
