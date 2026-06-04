package com.emt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tournament_participant")
public class TournamentParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tournamentParticipantId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "tournament_id")
  private Tournament tournament;

  @ManyToOne(optional = false)
  @JoinColumn(name = "player_id")
  private Player player;

  @NotNull private Integer seedNumber;
}
