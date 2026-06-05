package com.emt.mapper;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.entity.TournamentParticipant;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentMatchResponse;
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
        .status(tournament.getStatus())
        .winnerId(playerId(tournament.getWinner()))
        .winnerNickname(nickname(tournament.getWinner()))
        .createdAt(tournament.getCreatedAt())
        .startedAt(tournament.getStartedAt())
        .completedAt(tournament.getCompletedAt())
        .participants(mapParticipants(tournament))
        .matches(mapMatches(tournament))
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

  private List<TournamentMatchResponse> mapMatches(Tournament tournament) {
    return tournament.getMatches().stream().map(this::mapMatch).toList();
  }

  private TournamentMatchResponse mapMatch(TournamentMatch match) {
    return TournamentMatchResponse.builder()
        .tournamentMatchId(match.getTournamentMatchId())
        .roundNumber(match.getRoundNumber())
        .matchNumber(match.getMatchNumber())
        .status(match.getStatus())
        .firstPlayerId(match.getFirstPlayer().getPlayerId())
        .firstPlayerNickname(match.getFirstPlayer().getNickname())
        .secondPlayerId(match.getSecondPlayer().getPlayerId())
        .secondPlayerNickname(match.getSecondPlayer().getNickname())
        .winnerId(playerId(match.getWinner()))
        .winnerNickname(nickname(match.getWinner()))
        .completedAt(match.getCompletedAt())
        .build();
  }

  private Long playerId(Player player) {
    return player == null ? null : player.getPlayerId();
  }

  private String nickname(Player player) {
    return player == null ? null : player.getNickname();
  }
}
