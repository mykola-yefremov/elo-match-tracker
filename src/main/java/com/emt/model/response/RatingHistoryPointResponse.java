package com.emt.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record RatingHistoryPointResponse(
    Instant occurredAt, BigDecimal rating, Long matchId, String label) {}
