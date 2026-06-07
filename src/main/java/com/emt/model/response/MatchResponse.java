package com.emt.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record MatchResponse(
    Long matchId,
    Long winnerId,
    String winnerName,
    Long loserId,
    String loserName,
    Instant createdAt,
    BigDecimal winnerRatingChange,
    Integer winnerScore,
    Integer loserScore,
    String note) {

  public MatchResponse(
      Long matchId,
      String winnerName,
      String loserName,
      Instant createdAt,
      BigDecimal winnerRatingChange) {
    this(matchId, null, winnerName, null, loserName, createdAt, winnerRatingChange, null, null, null);
  }
}
