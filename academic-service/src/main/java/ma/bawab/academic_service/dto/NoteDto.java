package ma.bawab.academic_service.dto;

public class NoteDto {
    private Long id;
    private java.math.BigDecimal note1;
    private java.math.BigDecimal note2;
    private java.math.BigDecimal note3;

    public NoteDto() {
    }

    public NoteDto(Long id, java.math.BigDecimal note1, java.math.BigDecimal note2, java.math.BigDecimal note3) {
        this.id = id;
        this.note1 = note1;
        this.note2 = note2;
        this.note3 = note3;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public java.math.BigDecimal getNote1() {
        return note1;
    }

    public void setNote1(java.math.BigDecimal note1) {
        this.note1 = note1;
    }

    public java.math.BigDecimal getNote2() {
        return note2;
    }

    public void setNote2(java.math.BigDecimal note2) {
        this.note2 = note2;
    }

    public java.math.BigDecimal getNote3() {
        return note3;
    }

    public void setNote3(java.math.BigDecimal note3) {
        this.note3 = note3;
    }
}
