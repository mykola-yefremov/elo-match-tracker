package com.emt.mapper;

import com.emt.entity.Match;
import com.emt.entity.Player;
import com.emt.model.request.CreateMatchRequest;
import com.emt.model.response.MatchResponse;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchMapper {

  public Match mapToEntity(
      Player winner, Player loser, BigDecimal winnerRatingChange, CreateMatchRequest request) {
    return Match.builder()
        .winner(winner)
        .loser(loser)
        .winnerRatingChange(winnerRatingChange)
        .winnerScore(request.winnerScore())
        .loserScore(request.loserScore())
        .note(normalizedNote(request.note()))
        .createdAt(Instant.now())
        .build();
  }

  public Match mapToEntity(Player winner, Player loser, BigDecimal winnerRatingChange) {
    return mapToEntity(
        winner,
        loser,
        winnerRatingChange,
        CreateMatchRequest.builder().winnerId(winner.getPlayerId()).loserId(loser.getPlayerId()).build());
  }

  public MatchResponse mapToResponse(Match match) {
    return MatchResponse.builder()
        .matchId(match.getMatchId())
        .winnerId(match.getWinner().getPlayerId())
        .winnerName(match.getWinner().getNickname())
        .loserId(match.getLoser().getPlayerId())
        .loserName(match.getLoser().getNickname())
        .winnerRatingChange(match.getWinnerRatingChange())
        .winnerScore(match.getWinnerScore())
        .loserScore(match.getLoserScore())
        .note(match.getNote())
        .createdAt(match.getCreatedAt())
        .build();
  }

  private String normalizedNote(String note) {
    if (note == null || note.isBlank()) {
      return null;
    }
    return note.strip();
  }
}
