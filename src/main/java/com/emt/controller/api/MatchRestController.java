package com.emt.controller.api;

import com.emt.model.api.PageResponse;
import com.emt.model.request.CreateMatchRequest;
import com.emt.model.response.MatchResponse;
import com.emt.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Report matches, filter history, and cancel match records.")
public class MatchRestController {

  private final MatchService matchService;

  @GetMapping
  @Operation(
      summary = "List match history",
      description = "Returns match history, optionally filtered by one player or by a player pair.")
  public PageResponse<MatchResponse> getMatches(
      @RequestParam(required = false) Long playerId,
      @RequestParam(required = false) Long opponentId,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return PageResponse.from(matchService.getMatchHistory(playerId, opponentId, pageable));
  }

  @GetMapping("/{matchId}")
  @Operation(summary = "Get match", description = "Returns one match with score, note, and Elo delta.")
  public MatchResponse getMatch(@PathVariable Long matchId) {
    return matchService.getMatch(matchId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Report match", description = "Creates a match and updates Elo ratings.")
  public MatchResponse createMatch(@Valid @RequestBody CreateMatchRequest request) {
    return matchService.createMatch(request);
  }

  @DeleteMapping("/{matchId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Cancel match", description = "Deletes a match and repairs affected Elo history.")
  public void cancelMatch(@PathVariable Long matchId) {
    matchService.cancelMatch(matchId);
  }
}
