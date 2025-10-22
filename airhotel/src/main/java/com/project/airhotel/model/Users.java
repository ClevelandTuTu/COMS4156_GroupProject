package com.project.airhotel.model;

import com.project.airhotel.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_users_email", columnNames = "email")
})
public class Users {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role = UserRole.guest;
  @Column()
  private String phone;
  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime created_at;
  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updated_at;

  @PrePersist
  void prePersist() {
    if (role == null) {
      role = UserRole.guest;
    }
  }
}
