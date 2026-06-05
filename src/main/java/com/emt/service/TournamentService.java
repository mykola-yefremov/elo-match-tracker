package com.emt.service;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.mapper.TournamentMapper;
import com.emt.metrics.BusinessMetrics;
import com.emt.model.exception.TournamentCreationException;
import com.emt.model.request.CreateMatchRequest;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.SeedingMode;
import com.emt.model.tournament.TournamentMatchStatus;
import com.emt.model.tournament.TournamentStatus;
import com.emt.repository.PlayerRepository;
import com.emt.repository.TournamentMatchRepository;
import com.emt.repository.TournamentRepository;
import com.emt.service.tournament.TournamentBracketStrategy;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentService {

  private static final List<Integer> SUPPORTED_PLAYER_COUNTS = List.of(2, 4, 8, 16);

  private final PlayerRepository playerRepository;
  private final TournamentRepository tournamentRepository;
  private final TournamentMatchRepository tournamentMatchRepository;
  private final TournamentMapper tournamentMapper;
  private final MatchService matchService;
  private final BusinessMetrics businessMetrics;
  private final Clock clock;
  private final List<TournamentBracketStrategy> bracketStrategies;

  @Transactional(readOnly = true)
  public List<TournamentResponse> getAllTournaments() {
    return tournamentRepository.findAllWithParticipants().stream()
        .map(tournamentMapper::mapToResponse)
        .toList();
  }

  @Transactional
  public TournamentResponse createTournament(CreateTournamentRequest request) {
    validateRequest(request);

    List<Player> players = new ArrayList<>(selectedPlayers(request.playerIds()));
    if (request.seedingMode() == SeedingMode.RANDOM) {
      // Random seeding is intentionally non-deterministic; saved seeds are the source of truth.
      Collections.shuffle(players);
    }

    TournamentResponse response =
        Optional.of(tournamentMapper.mapToEntity(request, players))
            .map(tournamentRepository::save)
            .map(tournamentMapper::mapToResponse)
            .orElseThrow();
    businessMetrics.recordTournamentCreated();
    return response;
  }

  @Transactional
  public TournamentResponse startTournament(Long tournamentId) {
    Tournament tournament = getTournament(tournamentId);
    validateTournamentCanStart(tournament);

    bracketStrategy(tournament.getBracketType()).createInitialMatches(tournament);
    tournament.setStatus(TournamentStatus.ACTIVE);
    tournament.setStartedAt(Instant.now(clock));

    return tournamentMapper.mapToResponse(tournamentRepository.save(tournament));
  }

  @Transactional
  public TournamentResponse reportTournamentMatchResult(Long tournamentMatchId, Long winnerId) {
    TournamentMatch tournamentMatch = getTournamentMatch(tournamentMatchId);
    Tournament tournament = tournamentMatch.getTournament();
    validateMatchCanBeReported(tournament, tournamentMatch, winnerId);

    Player winner = playerInMatch(tournamentMatch, winnerId);
    Player loser = loserInMatch(tournamentMatch, winnerId);
    matchService.createMatch(
        CreateMatchRequest.builder()
            .winnerId(winner.getPlayerId())
            .loserId(loser.getPlayerId())
            .build());

    completeTournamentMatch(tournamentMatch, winner);
    bracketStrategy(tournament.getBracketType())
        .progressAfterResult(tournament, tournamentMatch.getRoundNumber());

    return tournamentMapper.mapToResponse(tournamentRepository.save(tournament));
  }

  public List<Integer> supportedPlayerCounts() {
    return SUPPORTED_PLAYER_COUNTS;
  }

  private Tournament getTournament(Long tournamentId) {
    return tournamentRepository
        .findById(tournamentId)
        .orElseThrow(() -> new TournamentCreationException("Tournament not found: " + tournamentId));
  }

  private TournamentMatch getTournamentMatch(Long tournamentMatchId) {
    return tournamentMatchRepository
        .findWithPlayersByTournamentMatchId(tournamentMatchId)
        .orElseThrow(
            () -> new TournamentCreationException("Tournament match not found: " + tournamentMatchId));
  }

  private TournamentBracketStrategy bracketStrategy(BracketType bracketType) {
    return bracketStrategies.stream()
        .filter(strategy -> strategy.bracketType() == bracketType)
        .findFirst()
        .orElseThrow(
            () -> new TournamentCreationException("Unsupported bracket type: " + bracketType));
  }

  private void validateTournamentCanStart(Tournament tournament) {
    if (tournament.getStatus() != TournamentStatus.DRAFT) {
      throw new TournamentCreationException("Only draft tournaments can be started.");
    }
    if (!tournament.getMatches().isEmpty()) {
      throw new TournamentCreationException("Tournament bracket has already been generated.");
    }
    if (tournament.getParticipants().size() != tournament.getPlayerCount()) {
      throw new TournamentCreationException("Tournament roster is incomplete.");
    }
  }

  private void validateMatchCanBeReported(
      Tournament tournament, TournamentMatch tournamentMatch, Long winnerId) {
    if (tournament.getStatus() != TournamentStatus.ACTIVE) {
      throw new TournamentCreationException("Only active tournaments can receive match results.");
    }
    if (tournamentMatch.getStatus() != TournamentMatchStatus.PENDING) {
      throw new TournamentCreationException("Tournament match has already been completed.");
    }
    if (!playerBelongsToMatch(tournamentMatch, winnerId)) {
      throw new TournamentCreationException("Winner must be one of the tournament match players.");
    }
  }

  private boolean playerBelongsToMatch(TournamentMatch tournamentMatch, Long playerId) {
    return tournamentMatch.getFirstPlayer().getPlayerId().equals(playerId)
        || tournamentMatch.getSecondPlayer().getPlayerId().equals(playerId);
  }

  private Player playerInMatch(TournamentMatch tournamentMatch, Long playerId) {
    if (tournamentMatch.getFirstPlayer().getPlayerId().equals(playerId)) {
      return tournamentMatch.getFirstPlayer();
    }
    return tournamentMatch.getSecondPlayer();
  }

  private Player loserInMatch(TournamentMatch tournamentMatch, Long winnerId) {
    if (tournamentMatch.getFirstPlayer().getPlayerId().equals(winnerId)) {
      return tournamentMatch.getSecondPlayer();
    }
    return tournamentMatch.getFirstPlayer();
  }

  private void completeTournamentMatch(TournamentMatch tournamentMatch, Player winner) {
    tournamentMatch.setWinner(winner);
    tournamentMatch.setStatus(TournamentMatchStatus.COMPLETED);
    tournamentMatch.setCompletedAt(Instant.now(clock));
  }

  private void validateRequest(CreateTournamentRequest request) {
    validateSupportedPlayerCount(request.playerCount());
    validateRoster(request.playerIds(), request.playerCount());
  }

  private void validateSupportedPlayerCount(Integer playerCount) {
    if (!SUPPORTED_PLAYER_COUNTS.contains(playerCount)) {
      throw new TournamentCreationException("Unsupported player count: " + playerCount);
    }
  }

  private void validateRoster(List<Long> playerIds, Integer expectedPlayerCount) {
    if (playerIds == null || playerIds.isEmpty()) {
      throw new TournamentCreationException("Select players for the tournament.");
    }
    if (hasDuplicatePlayerIds(playerIds)) {
      throw new TournamentCreationException("A player can only be selected once.");
    }
    if (playerIds.size() != expectedPlayerCount) {
      throw new TournamentCreationException(
          "Expected %s players, got %s.".formatted(expectedPlayerCount, playerIds.size()));
    }
  }

  private boolean hasDuplicatePlayerIds(List<Long> playerIds) {
    return playerIds.stream().distinct().count() != playerIds.size();
  }

  private List<Player> selectedPlayers(List<Long> playerIds) {
    List<Player> foundPlayers = playerRepository.findAllById(playerIds);
    validateAllPlayersExist(playerIds, foundPlayers);

    return playerIds.stream().map(playerId -> selectedPlayer(foundPlayers, playerId)).toList();
  }

  private void validateAllPlayersExist(List<Long> playerIds, List<Player> foundPlayers) {
    List<Long> missingPlayerIds =
        playerIds.stream().filter(playerId -> playerNotFound(foundPlayers, playerId)).toList();
    if (!missingPlayerIds.isEmpty()) {
      throw new TournamentCreationException("Players not found with ids: " + missingPlayerIds);
    }
  }

  private boolean playerNotFound(List<Player> players, Long playerId) {
    return players.stream().noneMatch(player -> player.getPlayerId().equals(playerId));
  }

  private Player selectedPlayer(List<Player> players, Long playerId) {
    return players.stream()
        .filter(player -> player.getPlayerId().equals(playerId))
        .findFirst()
        .orElseThrow();
  }
}
