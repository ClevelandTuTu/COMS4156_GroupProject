package com.project.airhotel.user.domain;

import com.project.airhotel.common.model.ModelConstants;
import com.project.airhotel.user.domain.enums.LoyaltyTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Loyalty program account attached to a user. Tracks tier, points and
 * stay/spend statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loyalty_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_loyalty_user", columnNames = "user_id")
})
public class LoyaltyAccounts {

  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Owning user id.
   */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /**
   * Loyalty tier.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private LoyaltyTier tier;

  /**
   * Current points balance.
   */
  @Column(nullable = false)
  private Integer points;

  /**
   * Lifetime nights stayed.
   */
  @Column(name = "lifetime_nights", nullable = false)
  private Integer lifetimeNights;

  /**
   * Lifetime spend in USD.
   */
  @Column(name = "lifetime_spend_usd", nullable = false,
      precision = ModelConstants.P12, scale = ModelConstants.S2)
  private BigDecimal lifetimeSpendUsd;

  /**
   * Nights stayed in the current year.
   */
  @Column(name = "annual_nights", nullable = false)
  private Integer annualNights;

  /**
   * Spend in USD in the current year.
   */
  @Column(name = "annual_spend_usd", nullable = false,
      precision = ModelConstants.P12, scale = ModelConstants.S2)
  private BigDecimal annualSpendUsd;

  /**
   * Date of the last completed stay.
   */
  @Column(name = "last_stay_at")
  private LocalDate lastStayAt;
}
