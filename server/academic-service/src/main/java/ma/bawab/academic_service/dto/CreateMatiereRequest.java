package ma.bawab.academic_service.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateMatiereRequest {

    @NotBlank
    private String nomMatiere;

    public String getNomMatiere() {
        return nomMatiere;
    }

    public void setNomMatiere(String nomMatiere) {
        this.nomMatiere = nomMatiere;
    }
}
