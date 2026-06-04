package com.emt.service;

import com.emt.entity.Player;
import com.emt.mapper.TournamentMapper;
import com.emt.model.exception.TournamentCreationException;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.SeedingMode;
import com.emt.repository.PlayerRepository;
import com.emt.repository.TournamentRepository;
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
  private final TournamentMapper tournamentMapper;

  @Transactional(readOnly = true)
  public List<TournamentResponse> getAllTournaments() {
    return tournamentRepository.findAllWithParticipants().stream()
        .map(tournamentMapper::mapToResponse)
        .toList();
  }

  @Transactional
  public TournamentResponse createTournament(CreateTournamentRequest request) {
    validateRequest(request);

    List<Player> players = selectedPlayers(request.playerIds());
    if (request.seedingMode() == SeedingMode.RANDOM) {
      Collections.shuffle(players);
    }

    return Optional.of(tournamentMapper.mapToEntity(request, players))
        .map(tournamentRepository::save)
        .map(tournamentMapper::mapToResponse)
        .orElseThrow();
  }

  public List<Integer> supportedPlayerCounts() {
    return SUPPORTED_PLAYER_COUNTS;
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
    List<Player> players = new ArrayList<>();
    for (Long playerId : playerIds) {
      Player player =
          playerRepository
              .findById(playerId)
              .orElseThrow(
                  () -> new TournamentCreationException("Player not found with id " + playerId));
      players.add(player);
    }
    return players;
  }
}
