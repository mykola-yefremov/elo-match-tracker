package com.emt.controller.api;

import com.emt.model.api.PageResponse;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.PlayerResponse;
import com.emt.service.PlayerService;
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
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Register players and read the Elo leaderboard.")
public class PlayerRestController {

  private final PlayerService playerService;

  @GetMapping
  @Operation(summary = "List players", description = "Returns players ordered by Elo rating.")
  public PageResponse<PlayerResponse> getPlayers(
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return PageResponse.from(playerService.getPlayers(pageable));
  }

  @GetMapping("/{playerId}")
  @Operation(summary = "Get player", description = "Returns one player by id.")
  public PlayerResponse getPlayer(@PathVariable Long playerId) {
    return playerService.getPlayerResponseById(playerId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create player", description = "Registers a player with the default Elo rating.")
  public PlayerResponse createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
    return playerService.createPlayer(request);
  }
}
