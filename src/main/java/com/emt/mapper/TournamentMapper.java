package com.emt.mapper;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentParticipant;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentParticipantResponse;
import com.emt.model.response.TournamentResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

  public Tournament mapToEntity(CreateTournamentRequest request, List<Player> seededPlayers) {
    Tournament tournament =
        Tournament.builder()
            .name(request.name().trim())
            .playerCount(request.playerCount())
            .seedingMode(request.seedingMode())
            .gameFormat(request.gameFormat())
            .winningPoints(request.winningPoints())
            .bracketType(request.bracketType())
            .createdAt(Instant.now())
            .build();

    for (int i = 0; i < seededPlayers.size(); i++) {
      tournament
          .getParticipants()
          .add(
              TournamentParticipant.builder()
                  .tournament(tournament)
                  .player(seededPlayers.get(i))
                  .seedNumber(i + 1)
                  .build());
    }

    return tournament;
  }

  public TournamentResponse mapToResponse(Tournament tournament) {
    return TournamentResponse.builder()
        .tournamentId(tournament.getTournamentId())
        .name(tournament.getName())
        .playerCount(tournament.getPlayerCount())
        .seedingMode(tournament.getSeedingMode())
        .gameFormat(tournament.getGameFormat())
        .winningPoints(tournament.getWinningPoints())
        .bracketType(tournament.getBracketType())
        .createdAt(tournament.getCreatedAt())
        .participants(mapParticipants(tournament))
        .build();
  }

  private List<TournamentParticipantResponse> mapParticipants(Tournament tournament) {
    return tournament.getParticipants().stream()
        .map(this::mapParticipant)
        .toList();
  }

  private TournamentParticipantResponse mapParticipant(TournamentParticipant participant) {
    return TournamentParticipantResponse.builder()
        .seedNumber(participant.getSeedNumber())
        .playerId(participant.getPlayer().getPlayerId())
        .nickname(participant.getPlayer().getNickname())
        .build();
  }
}
