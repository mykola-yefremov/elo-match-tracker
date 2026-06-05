package com.emt.entity;

import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import com.emt.model.tournament.TournamentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tournament")
public class Tournament {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tournamentId;

  @NotNull private String name;
  @NotNull private Integer playerCount;

  @NotNull
  @Enumerated(EnumType.STRING)
  private SeedingMode seedingMode;

  @NotNull
  @Enumerated(EnumType.STRING)
  private GameFormat gameFormat;

  @NotNull private Integer winningPoints;

  @NotNull
  @Enumerated(EnumType.STRING)
  private BracketType bracketType;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TournamentStatus status = TournamentStatus.DRAFT;

  @ManyToOne
  @JoinColumn(name = "winner_id")
  private Player winner;

  @NotNull private Instant createdAt;
  private Instant startedAt;
  private Instant completedAt;

  @Builder.Default
  @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("seedNumber ASC")
  private List<TournamentParticipant> participants = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("roundNumber ASC, matchNumber ASC")
  private List<TournamentMatch> matches = new ArrayList<>();
}
