package com.emt.entity;

import static jakarta.persistence.FetchType.LAZY;

import com.emt.model.tournament.TournamentMatchStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "tournament_match")
public class TournamentMatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tournamentMatchId;

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "tournament_id")
  private Tournament tournament;

  @NotNull private Integer roundNumber;
  @NotNull private Integer matchNumber;

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "first_player_id")
  private Player firstPlayer;

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "second_player_id")
  private Player secondPlayer;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "winner_id")
  private Player winner;

  @NotNull
  @Enumerated(EnumType.STRING)
  private TournamentMatchStatus status;

  @NotNull private Instant createdAt;
  private Instant completedAt;
}
