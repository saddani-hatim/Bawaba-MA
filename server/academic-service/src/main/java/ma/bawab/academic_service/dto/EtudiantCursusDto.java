package ma.bawab.academic_service.dto;

public class EtudiantCursusDto {
    private Long id;
    private String anneeScolaire;
    private String niveau;
    private boolean isCurrent;

    public EtudiantCursusDto() {
    }

    public EtudiantCursusDto(Long id, String anneeScolaire, String niveau, boolean isCurrent) {
        this.id = id;
        this.anneeScolaire = anneeScolaire;
        this.niveau = niveau;
        this.isCurrent = isCurrent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}
