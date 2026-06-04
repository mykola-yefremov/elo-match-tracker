package com.emt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player")
public class Player {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long playerId;

  @NotNull private String nickname;
  @NotNull @Builder.Default private BigDecimal eloRating = new BigDecimal("1200");
  @NotNull private Instant registeredAt;

  @Version @Builder.Default private Long version = 0L;

  public Player(Long playerId, String nickname, BigDecimal eloRating, Instant registeredAt) {
    this.playerId = playerId;
    this.nickname = nickname;
    this.eloRating = eloRating;
    this.registeredAt = registeredAt;
    this.version = 0L;
  }
}
