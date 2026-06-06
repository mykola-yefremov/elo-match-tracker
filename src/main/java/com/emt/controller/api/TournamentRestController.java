package com.emt.controller.api;

import com.emt.model.api.PageResponse;
import com.emt.model.api.TournamentMatchResultRequest;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentResponse;
import com.emt.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournaments", description = "Create tournaments, start brackets, and report winners.")
public class TournamentRestController {

  private final TournamentService tournamentService;

  @GetMapping
  @Operation(summary = "List tournaments", description = "Returns tournaments with participants and matches.")
  public PageResponse<TournamentResponse> getTournaments(
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return PageResponse.from(tournamentService.getTournaments(pageable));
  }

  @GetMapping("/{tournamentId}")
  @Operation(summary = "Get tournament", description = "Returns a tournament with participants and matches.")
  public TournamentResponse getTournament(@PathVariable Long tournamentId) {
    return tournamentService.getTournamentById(tournamentId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create tournament", description = "Creates a draft tournament with seeded players.")
  public TournamentResponse createTournament(@Valid @RequestBody CreateTournamentRequest request) {
    return tournamentService.createTournament(request);
  }

  @PostMapping("/{tournamentId}/start")
  @Operation(summary = "Start tournament", description = "Generates the tournament match schedule.")
  public TournamentResponse startTournament(@PathVariable Long tournamentId) {
    return tournamentService.startTournament(tournamentId);
  }

  @PostMapping("/matches/{tournamentMatchId}/result")
  @Operation(
      summary = "Report tournament match result",
      description = "Records a tournament winner and creates a normal Elo match.")
  public TournamentResponse reportTournamentMatchResult(
      @PathVariable Long tournamentMatchId,
      @Valid @RequestBody TournamentMatchResultRequest request) {
    return tournamentService.reportTournamentMatchResult(tournamentMatchId, request.winnerId());
  }
}
