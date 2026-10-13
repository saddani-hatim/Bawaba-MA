package ma.bawab.academic_service.dto;

public class ParentDto {
    private Long id;
    private Long userId;
    private String nom;
    private String prenom;
    private String telephone;

    public ParentDto() {
    }

    public ParentDto(Long id, Long userId, String nom, String prenom, String telephone) {
        this.id = id;
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
