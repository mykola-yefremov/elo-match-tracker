package com.emt.service;

import static ch.obermuhlner.math.big.BigDecimalMath.pow;
import static java.math.BigDecimal.ONE;
import static java.math.MathContext.DECIMAL128;
import static java.math.RoundingMode.HALF_UP;

import com.emt.entity.Match;
import com.emt.entity.Player;
import com.emt.mapper.MatchMapper;
import com.emt.metrics.BusinessMetrics;
import com.emt.model.exception.IdenticalPlayersException;
import com.emt.model.exception.InvalidMatchScoreException;
import com.emt.model.exception.MatchNotFoundException;
import com.emt.model.request.CreateMatchRequest;
import com.emt.model.response.MatchResponse;
import com.emt.repository.MatchRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchService {

  private static final BigDecimal CONSTANT_K = new BigDecimal("30");
  private final MatchRepository matchRepository;
  private final MatchMapper matchMapper;
  private final PlayerService playerService;
  private final BusinessMetrics businessMetrics;

  @Transactional(readOnly = true)
  public List<MatchResponse> getAllMatches() {
    return getMatchHistory(null, null);
  }

  @Transactional(readOnly = true)
  public List<MatchResponse> getMatchHistory(Long playerId, Long opponentId) {
    return mapMatches(findMatchesForHistory(playerId, opponentId));
  }

  @Transactional(readOnly = true)
  public Page<MatchResponse> getMatchHistory(Long playerId, Long opponentId, Pageable pageable) {
    return findMatchesForHistory(playerId, opponentId, newestFirst(pageable))
        .map(matchMapper::mapToResponse);
  }

  @Transactional(readOnly = true)
  public MatchResponse getMatch(Long matchId) {
    return matchRepository
        .findByIdWithPlayers(matchId)
        .map(matchMapper::mapToResponse)
        .orElseThrow(() -> new MatchNotFoundException(matchId));
  }

  private List<Match> findMatchesForHistory(Long playerId, Long opponentId) {
    MatchHistoryFilter filter = MatchHistoryFilter.from(playerId, opponentId);
    return filter.findWith(
        matchRepository::findAllWithPlayers,
        matchRepository::findMatchesByPlayer,
        pair -> matchRepository.findMatchesBetweenPlayers(pair.playerId(), pair.opponentId()));
  }

  private Page<Match> findMatchesForHistory(Long playerId, Long opponentId, Pageable pageable) {
    MatchHistoryFilter filter = MatchHistoryFilter.from(playerId, opponentId);
    return filter.findWith(
        () -> matchRepository.findAllWithPlayers(pageable),
        selectedPlayerId -> matchRepository.findMatchesByPlayer(selectedPlayerId, pageable),
        pair -> matchRepository.findMatchesBetweenPlayers(pair.playerId(), pair.opponentId(), pageable));
  }

  @Transactional
  public MatchResponse createMatch(CreateMatchRequest request) {
    if (request.winnerId().equals(request.loserId())) {
      throw new IdenticalPlayersException("A match cannot be created with identical players.");
    }
    validateScore(request);

    List<Player> players =
        playerService.getPlayersForRatingUpdate(request.winnerId(), request.loserId());
    Player winner = playerById(players, request.winnerId());
    Player loser = playerById(players, request.loserId());

    BigDecimal winnerRatingChange = updateEloRatings(winner, loser);

    MatchResponse response =
        Optional.of(matchMapper.mapToEntity(winner, loser, winnerRatingChange, request))
            .map(matchRepository::save)
            .map(matchMapper::mapToResponse)
            .orElseThrow();
    businessMetrics.recordMatchCreated();
    return response;
  }

  public BigDecimal updateEloRatings(Player winner, Player loser) {
    BigDecimal probabilityWinner =
        calculateProbability(winner.getEloRating(), loser.getEloRating());
    BigDecimal winnerRatingGain = CONSTANT_K.multiply(ONE.subtract(probabilityWinner));
    BigDecimal loserRatingLoss = winnerRatingGain.negate();

    winner.setEloRating(winner.getEloRating().add(winnerRatingGain));
    loser.setEloRating(loser.getEloRating().add(loserRatingLoss));

    playerService.saveWinnerAndLoser(winner, loser);

    return winnerRatingGain;
  }

  public BigDecimal calculateProbability(BigDecimal rating1, BigDecimal rating2) {
    BigDecimal exponent = rating2.subtract(rating1).divide(new BigDecimal("400"), 2, HALF_UP);
    BigDecimal divisor =
        ONE.add(pow(new BigDecimal("10"), exponent, DECIMAL128).setScale(2, HALF_UP));

    return ONE.divide(divisor, 2, HALF_UP);
  }

  @Transactional
  public void cancelMatch(Long matchId) {
    Match matchToCancel =
        matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));

    Long winnerId = matchToCancel.getWinner().getPlayerId();
    Long loserId = matchToCancel.getLoser().getPlayerId();
    List<Player> players = playerService.getPlayersForRatingUpdate(winnerId, loserId);
    Player winner = playerById(players, winnerId);
    Player loser = playerById(players, loserId);
    BigDecimal winnerRatingChange = matchToCancel.getWinnerRatingChange();

    winner.setEloRating(winner.getEloRating().subtract(winnerRatingChange));
    loser.setEloRating(loser.getEloRating().add(winnerRatingChange));

    playerService.saveWinnerAndLoser(winner, loser);

    List<Match> subsequentMatches =
        matchRepository.findMatchesByPlayersAfter(
            matchToCancel.getCreatedAt(), winnerId, loserId);

    recalculateEloRatingsForSubsequentMatches(subsequentMatches);

    matchRepository.deleteById(matchId);
    businessMetrics.recordMatchCancelled();
  }

  @Transactional
  public void recalculateEloRatingsForSubsequentMatches(List<Match> matches) {
    for (Match match : matches) {
      Long winnerId = match.getWinner().getPlayerId();
      Long loserId = match.getLoser().getPlayerId();
      List<Player> players = playerService.getPlayersForRatingUpdate(winnerId, loserId);
      Player winner = playerById(players, winnerId);
      Player loser = playerById(players, loserId);

      winner.setEloRating(winner.getEloRating().subtract(match.getWinnerRatingChange()));
      loser.setEloRating(loser.getEloRating().add(match.getWinnerRatingChange()));

      BigDecimal winnerRatingChange = updateEloRatings(winner, loser);

      match.setWinnerRatingChange(winnerRatingChange);

      matchRepository.save(match);
    }
  }

  private Pageable newestFirst(Pageable pageable) {
    return PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
  }

  private void validateScore(CreateMatchRequest request) {
    scoreError(request).ifPresent(
        message -> {
          throw new InvalidMatchScoreException(message);
        });
  }

  private Optional<String> scoreError(CreateMatchRequest request) {
    MatchScore score = MatchScore.from(request);
    return score.isPartial()
        ? Optional.of("Provide both winner and loser scores, or leave both empty.")
        : score.invalidWinnerScore();
  }

  private List<MatchResponse> mapMatches(List<Match> matches) {
    return matches.stream().map(matchMapper::mapToResponse).toList();
  }

  private Player playerById(List<Player> players, Long playerId) {
    return players.stream()
        .filter(player -> player.getPlayerId().equals(playerId))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Player with id %d not found in locked set".formatted(playerId)));
  }

  private sealed interface MatchHistoryFilter permits AllMatches, SinglePlayerMatches, PairMatches {
    static MatchHistoryFilter from(Long playerId, Long opponentId) {
      return Optional.ofNullable(selectedSinglePlayer(playerId, opponentId))
          .<MatchHistoryFilter>map(SinglePlayerMatches::new)
          .orElseGet(
              () ->
                  playerId == null && opponentId == null
                      ? new AllMatches()
                      : new PairMatches(playerId, opponentId));
    }

    private static Long selectedSinglePlayer(Long playerId, Long opponentId) {
      return playerId == null || opponentId == null || playerId.equals(opponentId)
          ? Optional.ofNullable(playerId).orElse(opponentId)
          : null;
    }

    <T> T findWith(
        Supplier<T> allMatches,
        Function<Long, T> singlePlayerMatches,
        Function<PairMatches, T> pairMatches);
  }

  private record AllMatches() implements MatchHistoryFilter {
    @Override
    public <T> T findWith(
        Supplier<T> allMatches,
        Function<Long, T> singlePlayerMatches,
        Function<PairMatches, T> pairMatches) {
      return allMatches.get();
    }
  }

  private record SinglePlayerMatches(Long playerId) implements MatchHistoryFilter {
    @Override
    public <T> T findWith(
        Supplier<T> allMatches,
        Function<Long, T> singlePlayerMatches,
        Function<PairMatches, T> pairMatches) {
      return singlePlayerMatches.apply(playerId);
    }
  }

  private record PairMatches(Long playerId, Long opponentId) implements MatchHistoryFilter {
    @Override
    public <T> T findWith(
        Supplier<T> allMatches,
        Function<Long, T> singlePlayerMatches,
        Function<PairMatches, T> pairMatches) {
      return pairMatches.apply(this);
    }
  }

  private record MatchScore(Integer winnerScore, Integer loserScore) {
    private static MatchScore from(CreateMatchRequest request) {
      return new MatchScore(request.winnerScore(), request.loserScore());
    }

    private boolean isPartial() {
      return (winnerScore == null) != (loserScore == null);
    }

    private Optional<String> invalidWinnerScore() {
      return winnerScore != null && winnerScore <= loserScore
          ? Optional.of("Winner score must be greater than loser score when both scores are provided.")
          : Optional.empty();
    }
  }
}
