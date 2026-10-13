package ma.bawab.academic_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false, unique = true)
    private Matiere matiere;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal note1 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal note2 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal note3 = BigDecimal.ZERO;

    public Note() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Matiere getMatiere() {
        return matiere;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
    }

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
