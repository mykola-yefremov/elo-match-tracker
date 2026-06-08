package com.emt.service;

import com.emt.entity.Player;
import com.emt.mapper.MatchMapper;
import com.emt.mapper.PlayerMapper;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.PlayerProfileResponse;
import com.emt.model.response.PlayerResponse;
import com.emt.model.response.PlayerStatsResponse;
import com.emt.model.response.RatingHistoryPointResponse;
import com.emt.repository.MatchRepository;
import com.emt.repository.PlayerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private static final BigDecimal INITIAL_ELO_RATING = new BigDecimal("1200");
  private static final int MATCH_PLAYER_COUNT = 2;
  private static final int RECENT_MATCH_LIMIT = 10;

  private final PlayerRepository playerRepository;
  private final MatchRepository matchRepository;
  private final PlayerMapper playerMapper;
  private final MatchMapper matchMapper;

  @Transactional(readOnly = true)
  public List<PlayerResponse> getAllPlayers() {
    return playerRepository.findAll().stream()
        .sorted(Comparator.comparing(Player::getEloRating).reversed())
        .map(playerMapper::mapToResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<PlayerResponse> getPlayers(Pageable pageable) {
    return playerRepository.findAllByRating(pageable).map(playerMapper::mapToResponse);
  }

  @Transactional(readOnly = true)
  public Page<PlayerResponse> searchPlayers(String query, Pageable pageable) {
    if (StringUtils.hasText(query)) {
      return playerRepository
          .searchByNickname(query.strip(), pageable)
          .map(playerMapper::mapToResponse);
    }
    return getPlayers(pageable);
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
  public PlayerResponse getPlayerResponseById(Long playerId) {
    return playerMapper.mapToResponse(getPlayerById(playerId));
  }

  @Transactional(readOnly = true)
  public PlayerProfileResponse getPlayerProfile(Long playerId) {
    Player player = getPlayerById(playerId);
    List<MatchResponse> matches =
        matchRepository.findMatchesByPlayer(playerId).stream()
            .map(matchMapper::mapToResponse)
            .toList();
    List<MatchResponse> matchesOldestFirst = new ArrayList<>(matches);
    Collections.reverse(matchesOldestFirst);

    return PlayerProfileResponse.builder()
        .player(playerMapper.mapToResponse(player))
        .stats(playerStats(playerId))
        .recentMatches(matches.stream().limit(RECENT_MATCH_LIMIT).toList())
        .ratingHistory(ratingHistory(player, matchesOldestFirst))
        .build();
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

  private PlayerStatsResponse playerStats(Long playerId) {
    int wins = matchRepository.countByWinner_PlayerId(playerId);
    int losses = matchRepository.countByLoser_PlayerId(playerId);
    int totalMatches = wins + losses;
    BigDecimal winRate =
        totalMatches == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalMatches), 2, RoundingMode.HALF_UP);

    return PlayerStatsResponse.builder()
        .wins(wins)
        .losses(losses)
        .totalMatches(totalMatches)
        .winRate(winRate)
        .build();
  }

  private List<RatingHistoryPointResponse> ratingHistory(
      Player player, List<MatchResponse> matchesOldestFirst) {
    BigDecimal rating = INITIAL_ELO_RATING;
    List<RatingHistoryPointResponse> history = new ArrayList<>();
    history.add(
        RatingHistoryPointResponse.builder()
            .occurredAt(player.getRegisteredAt())
            .rating(rating)
            .label("Registered")
            .build());

    for (MatchResponse match : matchesOldestFirst) {
      rating = rating.add(ratingDeltaFor(player.getPlayerId(), match));
      history.add(
          RatingHistoryPointResponse.builder()
              .occurredAt(match.createdAt())
              .rating(rating)
              .matchId(match.matchId())
              .label(opponentLabel(player.getPlayerId(), match))
              .build());
    }
    return history;
  }

  private BigDecimal ratingDeltaFor(Long playerId, MatchResponse match) {
    return match.winnerId().equals(playerId)
        ? match.winnerRatingChange()
        : match.winnerRatingChange().negate();
  }

  private String opponentLabel(Long playerId, MatchResponse match) {
    if (match.winnerId().equals(playerId)) {
      return "Won vs " + match.loserName();
    }
    return "Lost vs " + match.winnerName();
  }
}
