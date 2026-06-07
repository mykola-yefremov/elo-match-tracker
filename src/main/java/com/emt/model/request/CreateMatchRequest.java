package com.emt.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateMatchRequest(
    @NotNull Long winnerId,
    @NotNull Long loserId,
    @PositiveOrZero Integer winnerScore,
    @PositiveOrZero Integer loserScore,
    @Size(max = 500) String note) {}
