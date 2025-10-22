package com.project.airhotel.model;

import com.project.airhotel.model.enums.ClientType;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients", uniqueConstraints = {
    @UniqueConstraint(name = "uq_clients_apikey", columnNames = "api_key_hash")
})
public class Clients {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ClientType type;

  @Column(nullable = false, length = 200)
  private String api_key_hash;

  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime created_at;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updated_at;
}
