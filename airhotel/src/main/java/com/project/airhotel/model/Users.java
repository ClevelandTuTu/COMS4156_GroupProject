package com.project.airhotel.model;

import com.project.airhotel.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_users_email", columnNames = "email")
})
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String email;

    @Column(nullable=false, length=120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private UserRole role;

    @Column(nullable=false)
    private Integer phone;

    @CreationTimestamp
    @Column(nullable=false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    @Column(nullable=false)
    private LocalDateTime updated_at;
}