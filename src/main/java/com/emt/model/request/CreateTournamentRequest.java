package com.emt.model.request;

import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateTournamentRequest(
    @NotBlank(message = "Tournament name should not be blank.")
        @Size(max = 80, message = "Tournament name must not exceed 80 characters.")
        String name,
    @NotNull Integer playerCount,
    @NotNull SeedingMode seedingMode,
    @NotNull GameFormat gameFormat,
    @NotNull @Min(value = 1, message = "Winning points must be positive.") Integer winningPoints,
    @NotNull BracketType bracketType,
    List<Long> playerIds) {}
