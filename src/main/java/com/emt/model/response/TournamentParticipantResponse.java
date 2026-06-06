package com.emt.model.response;

import lombok.Builder;

@Builder
public record TournamentParticipantResponse(Integer seedNumber, Long playerId, String nickname) {}
