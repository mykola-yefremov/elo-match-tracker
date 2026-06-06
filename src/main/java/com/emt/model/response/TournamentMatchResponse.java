package com.emt.model.response;

import com.emt.model.tournament.TournamentMatchStatus;
import java.time.Instant;
import lombok.Builder;

@Builder
public record TournamentMatchResponse(
    Long tournamentMatchId,
    Integer roundNumber,
    Integer matchNumber,
    TournamentMatchStatus status,
    Long firstPlayerId,
    String firstPlayerNickname,
    Long secondPlayerId,
    String secondPlayerNickname,
    Long winnerId,
    String winnerNickname,
    Instant completedAt) {}
