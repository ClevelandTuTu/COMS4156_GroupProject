package com.project.airhotel.model;

import com.project.airhotel.model.enums.LoyaltyTier;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loyalty_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_loyalty_user", columnNames = "user_id")
})
public class LoyaltyAccounts {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long user_id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LoyaltyTier tier;

  @Column(nullable = false)
  private Integer points;

  @Column(nullable = false)
  private Integer lifetime_nights;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal lifetime_spend_usd;

  @Column(nullable = false)
  private Integer annual_nights;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal annual_spend_usd;

  private LocalDate last_stay_at;
}
