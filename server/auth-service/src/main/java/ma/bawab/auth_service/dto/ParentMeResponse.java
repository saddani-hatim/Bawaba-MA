package ma.bawab.auth_service.dto;

import ma.bawab.auth_service.entity.Role;
import java.time.LocalDateTime;

public class ParentMeResponse extends MeResponse {

    private String nom;
    private String prenom;
    private String telephone;

    public ParentMeResponse() {
    }

    public ParentMeResponse(Long id, String email, Role role, LocalDateTime createdAt,
            String nom, String prenom, String telephone) {
        super(id, email, role, createdAt);
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}
