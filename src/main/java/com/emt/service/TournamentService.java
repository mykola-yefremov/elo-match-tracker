package com.emt.service;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.mapper.TournamentMapper;
import com.emt.metrics.BusinessMetrics;
import com.emt.model.exception.TournamentCreationException;
import com.emt.model.exception.TournamentNotFoundException;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

  @Transactional(readOnly = true)
  public Page<TournamentResponse> getTournaments(Pageable pageable) {
    List<TournamentResponse> tournaments = getAllTournaments();
    int fromIndex = Math.min((int) pageable.getOffset(), tournaments.size());
    int toIndex = Math.min(fromIndex + pageable.getPageSize(), tournaments.size());
    return new PageImpl<>(tournaments.subList(fromIndex, toIndex), pageable, tournaments.size());
  }

  @Transactional
  public TournamentResponse createTournament(CreateTournamentRequest request) {
    validateRequest(request);

    List<Player> players = new ArrayList<>(selectedPlayers(request.playerIds()));
    seedPlayers(players, request.seedingMode());

    TournamentResponse response =
        Optional.of(tournamentMapper.mapToEntity(request, players))
            .map(tournamentRepository::save)
            .map(tournamentMapper::mapToResponse)
            .orElseThrow();
    businessMetrics.recordTournamentCreated();
    return response;
  }

  @Transactional(readOnly = true)
  public TournamentResponse getTournamentById(Long tournamentId) {
    return tournamentMapper.mapToResponse(getTournamentEntity(tournamentId));
  }

  @Transactional
  public TournamentResponse startTournament(Long tournamentId) {
    Tournament tournament = getTournamentEntity(tournamentId);
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
    Player loser = opponentInMatch(tournamentMatch, winnerId);
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

  private Tournament getTournamentEntity(Long tournamentId) {
    return tournamentRepository
        .findById(tournamentId)
        .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
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
    validate(
        List.of(
            rule(
                () -> tournament.getStatus() != TournamentStatus.DRAFT,
                "Only draft tournaments can be started."),
            rule(
                () -> !tournament.getMatches().isEmpty(),
                "Tournament bracket has already been generated."),
            rule(
                () -> tournament.getParticipants().size() != tournament.getPlayerCount(),
                "Tournament roster is incomplete.")));
  }

  private void validateMatchCanBeReported(
      Tournament tournament, TournamentMatch tournamentMatch, Long winnerId) {
    validate(
        List.of(
            rule(
                () -> tournament.getStatus() != TournamentStatus.ACTIVE,
                "Only active tournaments can receive match results."),
            rule(
                () -> tournamentMatch.getStatus() != TournamentMatchStatus.PENDING,
                "Tournament match has already been completed."),
            rule(
                () -> !playerBelongsToMatch(tournamentMatch, winnerId),
                "Winner must be one of the tournament match players.")));
  }

  private boolean playerBelongsToMatch(TournamentMatch tournamentMatch, Long playerId) {
    return playerSlots(tournamentMatch).containsKey(playerId);
  }

  private Player playerInMatch(TournamentMatch tournamentMatch, Long playerId) {
    return Optional.ofNullable(playerSlots(tournamentMatch).get(playerId)).orElseThrow();
  }

  private Player opponentInMatch(TournamentMatch tournamentMatch, Long playerId) {
    return playerSlots(tournamentMatch).entrySet().stream()
        .filter(entry -> !entry.getKey().equals(playerId))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
  }

  private Map<Long, Player> playerSlots(TournamentMatch tournamentMatch) {
    return Map.of(
        tournamentMatch.getFirstPlayer().getPlayerId(),
        tournamentMatch.getFirstPlayer(),
        tournamentMatch.getSecondPlayer().getPlayerId(),
        tournamentMatch.getSecondPlayer());
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
    validate(
        List.of(
            rule(
                () -> !SUPPORTED_PLAYER_COUNTS.contains(playerCount),
                "Unsupported player count: " + playerCount)));
  }

  private void validateRoster(List<Long> playerIds, Integer expectedPlayerCount) {
    validate(
        List.of(
            rule(
                () -> playerIds == null || playerIds.isEmpty(),
                "Select players for the tournament."),
            rule(() -> hasDuplicatePlayerIds(playerIds), "A player can only be selected once."),
            rule(
                () -> playerIds != null && playerIds.size() != expectedPlayerCount,
                () -> "Expected %s players, got %s.".formatted(expectedPlayerCount, playerIds.size()))));
  }

  private boolean hasDuplicatePlayerIds(List<Long> playerIds) {
    return playerIds != null && playerIds.stream().distinct().count() != playerIds.size();
  }

  private List<Player> selectedPlayers(List<Long> playerIds) {
    List<Player> foundPlayers = playerRepository.findAllById(playerIds);
    validateAllPlayersExist(playerIds, foundPlayers);

    return playerIds.stream().map(playerId -> selectedPlayer(foundPlayers, playerId)).toList();
  }

  private void validateAllPlayersExist(List<Long> playerIds, List<Player> foundPlayers) {
    List<Long> missingPlayerIds =
        playerIds.stream().filter(playerId -> playerNotFound(foundPlayers, playerId)).toList();
    validate(
        List.of(
            rule(
                () -> !missingPlayerIds.isEmpty(),
                "Players not found with ids: " + missingPlayerIds)));
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

  private void seedPlayers(List<Player> players, SeedingMode seedingMode) {
    Optional.ofNullable(seedingMode)
        .filter(SeedingMode.RANDOM::equals)
        .ifPresent(mode -> Collections.shuffle(players));
  }

  private ValidationRule rule(BooleanSupplier failed, String message) {
    return rule(failed, () -> message);
  }

  private ValidationRule rule(BooleanSupplier failed, Supplier<String> message) {
    return new ValidationRule(failed, message);
  }

  private void validate(List<ValidationRule> rules) {
    rules.stream()
        .filter(rule -> rule.failed().getAsBoolean())
        .findFirst()
        .ifPresent(
            rule -> {
              throw new TournamentCreationException(rule.message().get());
            });
  }

  private record ValidationRule(BooleanSupplier failed, Supplier<String> message) {}
}
