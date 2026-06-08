package com.emt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

  private static final BigDecimal CONSTANT_K = new BigDecimal("30");
  private static final BigDecimal DEFAULT_RATING_CHANGE = new BigDecimal("15.00");
  private static final String WINNER_NAME = "WinnerPlayer";
  private static final String LOSER_NAME = "LoserPlayer";

  @Mock private PlayerService playerService;
  @Mock private MatchRepository matchRepository;
  @Mock private MatchMapper matchMapper;
  @Mock private BusinessMetrics businessMetrics;
  @InjectMocks private MatchService matchService;

  @Test
  void getMatchHistory_withoutFilters_ShouldReturnAllMatches() {
    Match match = new Match();
    MatchResponse response =
        new MatchResponse(1L, WINNER_NAME, LOSER_NAME, Instant.now(), DEFAULT_RATING_CHANGE);

    given(matchRepository.findAllWithPlayers()).willReturn(List.of(match));
    given(matchMapper.mapToResponse(match)).willReturn(response);

    List<MatchResponse> actualResponses = matchService.getMatchHistory(null, null);

    assertThat(actualResponses).containsExactly(response);
    verify(matchRepository).findAllWithPlayers();
  }

  @Test
  void getMatchHistory_withSinglePlayerFilter_ShouldReturnPlayerMatches() {
    Match match = new Match();
    MatchResponse response =
        new MatchResponse(1L, WINNER_NAME, LOSER_NAME, Instant.now(), DEFAULT_RATING_CHANGE);

    given(matchRepository.findMatchesByPlayer(1L)).willReturn(List.of(match));
    given(matchMapper.mapToResponse(match)).willReturn(response);

    List<MatchResponse> actualResponses = matchService.getMatchHistory(1L, null);

    assertThat(actualResponses).containsExactly(response);
    verify(matchRepository).findMatchesByPlayer(1L);
    verify(matchRepository, never()).findMatchesBetweenPlayers(any(), any());
  }

  @Test
  void getMatchHistory_withOpponentOnlyFilter_ShouldReturnOpponentMatches() {
    Match match = new Match();
    MatchResponse response =
        new MatchResponse(1L, WINNER_NAME, LOSER_NAME, Instant.now(), DEFAULT_RATING_CHANGE);

    given(matchRepository.findMatchesByPlayer(2L)).willReturn(List.of(match));
    given(matchMapper.mapToResponse(match)).willReturn(response);

    List<MatchResponse> actualResponses = matchService.getMatchHistory(null, 2L);

    assertThat(actualResponses).containsExactly(response);
    verify(matchRepository).findMatchesByPlayer(2L);
  }

  @Test
  void getMatchHistory_withSamePlayerPairFilter_ShouldReturnSinglePlayerMatches() {
    Match match = new Match();
    MatchResponse response =
        new MatchResponse(1L, WINNER_NAME, LOSER_NAME, Instant.now(), DEFAULT_RATING_CHANGE);

    given(matchRepository.findMatchesByPlayer(1L)).willReturn(List.of(match));
    given(matchMapper.mapToResponse(match)).willReturn(response);

    List<MatchResponse> actualResponses = matchService.getMatchHistory(1L, 1L);

    assertThat(actualResponses).containsExactly(response);
    verify(matchRepository).findMatchesByPlayer(1L);
    verify(matchRepository, never()).findMatchesBetweenPlayers(any(), any());
  }

  @Test
  void getMatchHistory_withPairFilter_ShouldReturnMatchesBetweenPlayers() {
    Match match = new Match();
    MatchResponse response =
        new MatchResponse(1L, WINNER_NAME, LOSER_NAME, Instant.now(), DEFAULT_RATING_CHANGE);

    given(matchRepository.findMatchesBetweenPlayers(1L, 2L)).willReturn(List.of(match));
    given(matchMapper.mapToResponse(match)).willReturn(response);

    List<MatchResponse> actualResponses = matchService.getMatchHistory(1L, 2L);

    assertThat(actualResponses).containsExactly(response);
    verify(matchRepository).findMatchesBetweenPlayers(1L, 2L);
  }

  @Test
  void createMatch_WhenWinnerAndLoserAreIdentical_ShouldThrowException() {
    assertThatThrownBy(
            () ->
                matchService.createMatch(
                    CreateMatchRequest.builder().winnerId(1L).loserId(1L).build()))
        .isInstanceOf(IdenticalPlayersException.class)
        .hasMessageContaining("A match cannot be created with identical players.");
  }

  @Test
  void createMatch_WhenWinnerScoreIsNotGreater_ShouldThrowException() {
    assertThatThrownBy(
            () ->
                matchService.createMatch(
                    CreateMatchRequest.builder()
                        .winnerId(1L)
                        .loserId(2L)
                        .winnerScore(9)
                        .loserScore(11)
                        .build()))
        .isInstanceOf(InvalidMatchScoreException.class)
        .hasMessageContaining("Winner score must be greater than loser score");
  }

  @Test
  void createMatch_WhenOnlyOneScoreIsProvided_ShouldThrowException() {
    assertThatThrownBy(
            () ->
                matchService.createMatch(
                    CreateMatchRequest.builder().winnerId(1L).loserId(2L).winnerScore(11).build()))
        .isInstanceOf(InvalidMatchScoreException.class)
        .hasMessageContaining("Provide both winner and loser scores");
  }

  @Test
  void createMatch_WhenPlayersAreDifferent_ShouldCreateMatchSuccessfully() {
    CreateMatchRequest request = CreateMatchRequest.builder().winnerId(1L).loserId(2L).build();

    Player winner = new Player(1L, WINNER_NAME, new BigDecimal("2500"), Instant.now());
    Player loser = new Player(2L, LOSER_NAME, new BigDecimal("2000"), Instant.now());

    BigDecimal probabilityWinner =
        matchService.calculateProbability(winner.getEloRating(), loser.getEloRating());
    BigDecimal winnerRatingGain =
        CONSTANT_K
            .multiply(BigDecimal.ONE.subtract(probabilityWinner))
            .setScale(2, RoundingMode.HALF_UP);
    Match match = new Match();
    MatchResponse expectedResponse =
        new MatchResponse(1L, WINNER_NAME, LOSER_NAME, Instant.now(), winnerRatingGain);

    given(playerService.getPlayersForRatingUpdate(1L, 2L)).willReturn(List.of(winner, loser));
    given(matchMapper.mapToEntity(winner, loser, winnerRatingGain, request)).willReturn(match);
    given(matchRepository.save(match)).willReturn(match);
    given(matchMapper.mapToResponse(match)).willReturn(expectedResponse);

    MatchResponse actualResponse = matchService.createMatch(request);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    assertThat(actualResponse.winnerRatingChange()).isEqualByComparingTo(winnerRatingGain);
    verify(matchRepository).save(match);
    verify(businessMetrics).recordMatchCreated();
  }

  @Test
  void cancelMatch_WhenMatchExists_ShouldRevertRatingsAndDeleteMatch() {
    Instant createdAt = Instant.now();
    Player winner = new Player(1L, WINNER_NAME, new BigDecimal("1215.00"), createdAt);
    Player loser = new Player(2L, LOSER_NAME, new BigDecimal("1185.00"), createdAt);
    Match match = new Match(10L, winner, loser, DEFAULT_RATING_CHANGE, createdAt);

    given(matchRepository.findById(10L)).willReturn(Optional.of(match));
    given(playerService.getPlayersForRatingUpdate(1L, 2L)).willReturn(List.of(winner, loser));
    given(matchRepository.findMatchesByPlayersAfter(createdAt, 1L, 2L)).willReturn(List.of());

    matchService.cancelMatch(10L);

    assertThat(winner.getEloRating()).isEqualByComparingTo("1200.00");
    assertThat(loser.getEloRating()).isEqualByComparingTo("1200.00");
    verify(playerService).saveWinnerAndLoser(winner, loser);
    verify(matchRepository).deleteById(10L);
    verify(businessMetrics).recordMatchCancelled();
  }

  @Test
  void cancelMatch_WhenMatchDoesNotExist_ShouldThrowMatchNotFoundException() {
    given(matchRepository.findById(404L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> matchService.cancelMatch(404L))
        .isInstanceOf(MatchNotFoundException.class)
        .hasMessageContaining("Match not found with id 404");
  }

  @Test
  void recalculateEloRatingsForSubsequentMatches_ShouldReplaceStoredRatingChange() {
    Instant createdAt = Instant.now();
    Player winner = new Player(1L, WINNER_NAME, new BigDecimal("1215.00"), createdAt);
    Player loser = new Player(2L, LOSER_NAME, new BigDecimal("1185.00"), createdAt);
    Match match = new Match(20L, winner, loser, DEFAULT_RATING_CHANGE, createdAt);

    given(playerService.getPlayersForRatingUpdate(1L, 2L)).willReturn(List.of(winner, loser));

    matchService.recalculateEloRatingsForSubsequentMatches(List.of(match));

    assertThat(match.getWinnerRatingChange()).isEqualByComparingTo("15.00");
    assertThat(winner.getEloRating()).isEqualByComparingTo("1215.00");
    assertThat(loser.getEloRating()).isEqualByComparingTo("1185.00");
    verify(matchRepository).save(match);
  }

  @ParameterizedTest(
      name =
          "[{index}] Calculate Elo-Ranking for winner rating {0}, loser rating {1}, expected change {2}")
  @CsvSource({
    "1200, 1100, 10.80",
    "1200, 1250, 17.10",
    "1300, 1200, 10.80",
    "1500, 1200, 4.50",
    "1200, 1000, 7.20"
  })
  void calculateCorrectlyPlayersEloRating_WhenPlayersAreDifferent_ShouldCalculateCorrectlyEloRating(
      String winnerRatingStr, String loserRatingStr, String expectedChangeStr) {
    BigDecimal initialWinnerRating = new BigDecimal(winnerRatingStr);
    BigDecimal initialLoserRating = new BigDecimal(loserRatingStr);
    BigDecimal expectedChange = new BigDecimal(expectedChangeStr);

    Player winner = new Player(1L, WINNER_NAME, initialWinnerRating, Instant.now());
    Player loser = new Player(2L, LOSER_NAME, initialLoserRating, Instant.now());

    BigDecimal ratingChange = matchService.updateEloRatings(winner, loser);

    assertThat(ratingChange).isEqualByComparingTo(expectedChange);
  }
}
