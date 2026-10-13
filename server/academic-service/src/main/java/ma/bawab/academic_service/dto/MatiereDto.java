package ma.bawab.academic_service.dto;

public class MatiereDto {
    private Long id;
    private String nomMatiere;
    private NoteDto note;

    public MatiereDto() {
    }

    public MatiereDto(Long id, String nomMatiere, NoteDto note) {
        this.id = id;
        this.nomMatiere = nomMatiere;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomMatiere() {
        return nomMatiere;
    }

    public void setNomMatiere(String nomMatiere) {
        this.nomMatiere = nomMatiere;
    }

    public NoteDto getNote() {
        return note;
    }

    public void setNote(NoteDto note) {
        this.note = note;
    }
}
