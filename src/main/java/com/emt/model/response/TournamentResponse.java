package com.emt.model.response;

import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record TournamentResponse(
    Long tournamentId,
    String name,
    Integer playerCount,
    SeedingMode seedingMode,
    GameFormat gameFormat,
    Integer winningPoints,
    BracketType bracketType,
    Instant createdAt,
    List<TournamentParticipantResponse> participants) {}
