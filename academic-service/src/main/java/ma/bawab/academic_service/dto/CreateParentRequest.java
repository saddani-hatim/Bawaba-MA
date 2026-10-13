package ma.bawab.academic_service.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateParentRequest {

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    private String telephone;

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
