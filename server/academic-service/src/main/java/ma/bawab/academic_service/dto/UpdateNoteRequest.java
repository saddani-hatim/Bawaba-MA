package ma.bawab.academic_service.dto;

import java.math.BigDecimal;

public class UpdateNoteRequest {
    private BigDecimal note1;
    private BigDecimal note2;
    private BigDecimal note3;

    public BigDecimal getNote1() {
        return note1;
    }

    public void setNote1(BigDecimal note1) {
        this.note1 = note1;
    }

    public BigDecimal getNote2() {
        return note2;
    }

    public void setNote2(BigDecimal note2) {
        this.note2 = note2;
    }

    public BigDecimal getNote3() {
        return note3;
    }

    public void setNote3(BigDecimal note3) {
        this.note3 = note3;
    }
}
