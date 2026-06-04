package com.emt.entity;

import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
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

  @NotNull private Instant createdAt;

  @Builder.Default
  @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("seedNumber ASC")
  private List<TournamentParticipant> participants = new ArrayList<>();
}
