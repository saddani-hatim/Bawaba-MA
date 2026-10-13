package ma.bawab.auth_service.dto;

import ma.bawab.auth_service.entity.Role;
import java.time.LocalDateTime;

public class MeResponse {

    private Long id;
    private String email;
    private Role role;
    private LocalDateTime createdAt;

    public MeResponse() {
    }

    public MeResponse(Long id, String email, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
