package com.emt.service;

import com.emt.entity.Player;
import com.emt.mapper.PlayerMapper;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.PlayerResponse;
import com.emt.repository.PlayerRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private static final int MATCH_PLAYER_COUNT = 2;

  private final PlayerRepository playerRepository;

  private final PlayerMapper playerMapper;

  @Transactional(readOnly = true)
  public List<PlayerResponse> getAllPlayers() {
    return playerRepository.findAll().stream()
        .sorted(Comparator.comparing(Player::getEloRating).reversed())
        .map(playerMapper::mapToResponse)
        .toList();
  }

  @Transactional
  public PlayerResponse createPlayer(CreatePlayerRequest request) {
    if (playerRepository.existsByNickname(request.nickname())) {
      throw new PlayerAlreadyExistsException(request.nickname());
    }

    return Optional.of(request)
        .map(playerMapper::mapToEntity)
        .map(playerRepository::save)
        .map(playerMapper::mapToResponse)
        .orElseThrow();
  }

  @Transactional(readOnly = true)
  public Player getPlayerById(Long playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(() -> new PlayerNotFoundException(playerId));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public List<Player> getPlayersForRatingUpdate(Long firstPlayerId, Long secondPlayerId) {
    List<Player> players =
        playerRepository.findPlayersForUpdate(List.of(firstPlayerId, secondPlayerId));
    if (players.size() != MATCH_PLAYER_COUNT) {
      throw new PlayerNotFoundException(missingPlayerId(players, firstPlayerId, secondPlayerId));
    }
    return players;
  }

  @Transactional
  public List<Player> saveWinnerAndLoser(Player winner, Player loser) {
    return playerRepository.saveAll(List.of(winner, loser));
  }

  private Long missingPlayerId(List<Player> players, Long firstPlayerId, Long secondPlayerId) {
    return players.stream().noneMatch(player -> player.getPlayerId().equals(firstPlayerId))
        ? firstPlayerId
        : secondPlayerId;
  }
}
