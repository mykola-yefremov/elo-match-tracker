package com.emt.model.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TournamentMatchResultRequest(@NotNull Long winnerId) {}
