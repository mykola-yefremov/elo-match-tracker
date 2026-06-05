package com.emt.service;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.entity.TournamentParticipant;
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
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentService {

  private static final List<Integer> SUPPORTED_PLAYER_COUNTS = List.of(2, 4, 8, 16);
  private static final int FIRST_ROUND = 1;
  private static final int FINALIST_COUNT = 1;

  private final PlayerRepository playerRepository;
  private final TournamentRepository tournamentRepository;
  private final TournamentMatchRepository tournamentMatchRepository;
  private final TournamentMapper tournamentMapper;
  private final MatchService matchService;
  private final BusinessMetrics businessMetrics;

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

    if (tournament.getBracketType() == BracketType.SINGLE_ELIMINATION) {
      createSingleEliminationFirstRound(tournament);
    } else {
      createRoundRobinMatches(tournament);
    }

    tournament.setStatus(TournamentStatus.ACTIVE);
    tournament.setStartedAt(Instant.now(Clock.systemUTC()));
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
    progressTournament(tournament, tournamentMatch.getRoundNumber());

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

  private void createSingleEliminationFirstRound(Tournament tournament) {
    List<Player> seededPlayers = seededPlayers(tournament);
    for (int i = 0; i < seededPlayers.size() / 2; i++) {
      addTournamentMatch(
          tournament,
          FIRST_ROUND,
          i + 1,
          seededPlayers.get(i),
          seededPlayers.get(seededPlayers.size() - 1 - i));
    }
  }

  private void createRoundRobinMatches(Tournament tournament) {
    List<Player> rotation = new ArrayList<>(seededPlayers(tournament));
    int playerCount = rotation.size();

    for (int roundNumber = 1; roundNumber < playerCount; roundNumber++) {
      for (int i = 0; i < playerCount / 2; i++) {
        addTournamentMatch(
            tournament,
            roundNumber,
            i + 1,
            rotation.get(i),
            rotation.get(playerCount - 1 - i));
      }
      rotateRoundRobinPlayers(rotation);
    }
  }

  private void rotateRoundRobinPlayers(List<Player> players) {
    Player lastPlayer = players.remove(players.size() - 1);
    players.add(1, lastPlayer);
  }

  private void addTournamentMatch(
      Tournament tournament,
      Integer roundNumber,
      Integer matchNumber,
      Player firstPlayer,
      Player secondPlayer) {
    tournament
        .getMatches()
        .add(
            TournamentMatch.builder()
                .tournament(tournament)
                .roundNumber(roundNumber)
                .matchNumber(matchNumber)
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .status(TournamentMatchStatus.PENDING)
                .createdAt(Instant.now(Clock.systemUTC()))
                .build());
  }

  private List<Player> seededPlayers(Tournament tournament) {
    return tournament.getParticipants().stream()
        .sorted(Comparator.comparing(participant -> participant.getSeedNumber()))
        .map(participant -> participant.getPlayer())
        .toList();
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
    tournamentMatch.setCompletedAt(Instant.now(Clock.systemUTC()));
  }

  private void progressTournament(Tournament tournament, Integer completedRoundNumber) {
    if (tournament.getBracketType() == BracketType.ROUND_ROBIN) {
      completeRoundRobinTournamentIfReady(tournament);
      return;
    }
    progressSingleEliminationTournament(tournament, completedRoundNumber);
  }

  private void progressSingleEliminationTournament(
      Tournament tournament, Integer completedRoundNumber) {
    List<TournamentMatch> roundMatches = matchesInRound(tournament, completedRoundNumber);
    if (hasPendingMatches(roundMatches)) {
      return;
    }

    List<Player> winners = roundWinners(roundMatches);
    if (winners.size() == FINALIST_COUNT) {
      completeTournament(tournament, winners.get(0));
      return;
    }

    Integer nextRoundNumber = completedRoundNumber + 1;
    if (matchesInRound(tournament, nextRoundNumber).isEmpty()) {
      createNextSingleEliminationRound(tournament, nextRoundNumber, winners);
    }
  }

  private boolean hasPendingMatches(List<TournamentMatch> matches) {
    return matches.stream().anyMatch(match -> match.getStatus() != TournamentMatchStatus.COMPLETED);
  }

  private List<Player> roundWinners(List<TournamentMatch> matches) {
    return matches.stream()
        .sorted(Comparator.comparing(TournamentMatch::getMatchNumber))
        .map(TournamentMatch::getWinner)
        .toList();
  }

  private void createNextSingleEliminationRound(
      Tournament tournament, Integer roundNumber, List<Player> winners) {
    for (int i = 0; i < winners.size(); i += 2) {
      addTournamentMatch(tournament, roundNumber, (i / 2) + 1, winners.get(i), winners.get(i + 1));
    }
  }

  private List<TournamentMatch> matchesInRound(Tournament tournament, Integer roundNumber) {
    return tournament.getMatches().stream()
        .filter(match -> match.getRoundNumber().equals(roundNumber))
        .sorted(Comparator.comparing(TournamentMatch::getMatchNumber))
        .toList();
  }

  private void completeRoundRobinTournamentIfReady(Tournament tournament) {
    if (hasPendingMatches(tournament.getMatches())) {
      return;
    }

    Map<Long, Integer> winsByPlayerId = winsByPlayerId(tournament);
    Player winner =
        tournament.getParticipants().stream()
            .min(roundRobinRanking(winsByPlayerId))
            .map(participant -> participant.getPlayer())
            .orElseThrow();
    completeTournament(tournament, winner);
  }

  private Map<Long, Integer> winsByPlayerId(Tournament tournament) {
    Map<Long, Integer> winsByPlayerId = new LinkedHashMap<>();
    seededPlayers(tournament).forEach(player -> winsByPlayerId.put(player.getPlayerId(), 0));
    tournament
        .getMatches()
        .forEach(
            match -> winsByPlayerId.computeIfPresent(match.getWinner().getPlayerId(), (id, wins) -> wins + 1));
    return winsByPlayerId;
  }

  private Comparator<TournamentParticipant> roundRobinRanking(Map<Long, Integer> winsByPlayerId) {
    return Comparator
        .<TournamentParticipant>comparingInt(
            participant -> -winsByPlayerId.getOrDefault(participant.getPlayer().getPlayerId(), 0))
        .thenComparingInt(participant -> participant.getSeedNumber());
  }

  private void completeTournament(Tournament tournament, Player winner) {
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournament.setWinner(winner);
    tournament.setCompletedAt(Instant.now(Clock.systemUTC()));
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
