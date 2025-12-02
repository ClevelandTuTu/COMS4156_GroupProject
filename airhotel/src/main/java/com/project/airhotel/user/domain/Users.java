package com.project.airhotel.user.domain;

import com.project.airhotel.common.model.ModelConstants;
import com.project.airhotel.user.domain.enums.UserRole;
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


/**
 * Platform user account used for authentication and authorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_users_email", columnNames = "email")
})
public class Users {
  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Unique email address used for login.
   */
  @Column(nullable = false)
  private String email;

  /**
   * User display name.
   */
  @Column(nullable = false, length = ModelConstants.LEN_120)
  private String name;

  /**
   * Role controlling access permissions.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  @Builder.Default
  private UserRole role = UserRole.GUEST;

  /**
   * Optional phone number.
   */
  @Column()
  private String phone;

  /**
   * Creation timestamp managed by Hibernate.
   */
  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /**
   * Update timestamp managed by Hibernate.
   */
  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Ensure default role is set if not provided before insert.
   */
  @PrePersist
  void prePersist() {
    if (role == null) {
      role = UserRole.GUEST;
    }
  }
}
