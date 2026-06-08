package com.emt.model.response;

import java.util.List;
import lombok.Builder;

@Builder
public record PlayerProfileResponse(
    PlayerResponse player,
    PlayerStatsResponse stats,
    List<MatchResponse> recentMatches,
    List<RatingHistoryPointResponse> ratingHistory) {}
