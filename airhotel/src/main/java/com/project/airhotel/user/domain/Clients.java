package com.project.airhotel.user.domain;

import com.project.airhotel.common.model.ModelConstants;
import com.project.airhotel.user.domain.enums.ClientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Persisted client application that integrates with the platform. Holds
 * identification, type and hashed API key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients", uniqueConstraints = {
    @UniqueConstraint(name = "uq_clients_apikey", columnNames = "api_key_hash")
})
public class Clients {

  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Client display name.
   */
  @Column(nullable = false, length = ModelConstants.LEN_120)
  private String name;

  /**
   * Client category such as internal or partner.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private ClientType type;

  /**
   * Hash of the API key used for authentication.
   */
  @Column(nullable = false, length = ModelConstants.LEN_200)
  private String apiKeyHash;

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
}
