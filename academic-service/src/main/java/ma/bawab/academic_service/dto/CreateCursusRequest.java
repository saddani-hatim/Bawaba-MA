package ma.bawab.academic_service.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCursusRequest {

    @NotBlank
    private String anneeScolaire;

    @NotBlank
    private String niveau;

    public String getAnneeScolaire() {
        return anneeScolaire;
    }

    public void setAnneeScolaire(String anneeScolaire) {
        this.anneeScolaire = anneeScolaire;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }
}
