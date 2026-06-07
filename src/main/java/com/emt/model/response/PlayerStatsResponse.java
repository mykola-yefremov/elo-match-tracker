package com.emt.model.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PlayerStatsResponse(int wins, int losses, int totalMatches, BigDecimal winRate) {}
