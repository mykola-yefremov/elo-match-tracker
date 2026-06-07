package com.emt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.emt.ITBase;
import com.emt.entity.Player;
import com.emt.model.exception.MatchNotFoundException;
import com.emt.model.request.CreateMatchRequest;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.PlayerResponse;
import com.emt.repository.MatchRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
public class MatchServiceIT extends ITBase {

  private static final String WINNER_NICKNAME = "Winner";
  private static final String LOSER_NICKNAME = "Loser";

  private final MatchService matchService;
  private final MatchRepository matchRepository;
  private final PlayerService playerService;

  @Test
  public void testGetAllMatches_WhenMatchIsCreatedSuccessfully_ShouldReturnAllMatches() {
    PlayerResponse firstPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(WINNER_NICKNAME).build());
    PlayerResponse secondPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(LOSER_NICKNAME).build());

    CreateMatchRequest matchRequest =
        CreateMatchRequest.builder().winnerId(firstPlayer.playerId()).loserId(secondPlayer.playerId()).build();
    matchService.createMatch(matchRequest);

    List<MatchResponse> matches = matchService.getAllMatches();

    assertEquals(1, matches.size());
    assertEquals(WINNER_NICKNAME, matches.get(0).winnerName());
    assertEquals(LOSER_NICKNAME, matches.get(0).loserName());
  }

  @Test
  public void testEloRatingUpdateAfterMatch() {
    PlayerResponse firstPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(WINNER_NICKNAME).build());
    PlayerResponse secondPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(LOSER_NICKNAME).build());

    BigDecimal initialRatingFirstPlayer = firstPlayer.eloRating();
    BigDecimal initialRatingSecondPlayer = secondPlayer.eloRating();

    matchService.createMatch(
        CreateMatchRequest.builder().winnerId(firstPlayer.playerId()).loserId(secondPlayer.playerId()).build());

    Player updatedFirstPlayer = playerService.getPlayerById(firstPlayer.playerId());
    Player updatedSecondPlayer = playerService.getPlayerById(secondPlayer.playerId());

    assertThat(updatedFirstPlayer.getEloRating()).isGreaterThan(initialRatingFirstPlayer);
    assertThat(updatedSecondPlayer.getEloRating()).isLessThan(initialRatingSecondPlayer);
  }

  @Test
  public void testCancelNonExistentMatch_shouldThrowMatchNotFoundException() {
    MatchNotFoundException exception =
        assertThrows(MatchNotFoundException.class, () -> matchService.cancelMatch(999L));

    assertThat(exception.getMessage()).contains("Match not found with id");
  }

  @Test
  public void testCancelMatch_shouldHandleEloReversionAndMatchRemovalInComplexScenario() {
    PlayerResponse playerOne =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("PlayerOne").build());
    PlayerResponse playerTwo =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("PlayerTwo").build());
    PlayerResponse playerThree =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("PlayerThree").build());
    PlayerResponse playerFour =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("PlayerFour").build());

    MatchResponse firstMatch =
        matchService.createMatch(
            CreateMatchRequest.builder().winnerId(playerOne.playerId()).loserId(playerTwo.playerId()).build());
    MatchResponse secondMatch =
        matchService.createMatch(
            CreateMatchRequest.builder().winnerId(playerTwo.playerId()).loserId(playerThree.playerId()).build());
    MatchResponse thirdMatch =
        matchService.createMatch(
            CreateMatchRequest.builder().winnerId(playerThree.playerId()).loserId(playerFour.playerId()).build());

    matchService.cancelMatch(secondMatch.matchId());

    Player updatedPlayerOne = playerService.getPlayerById(playerOne.playerId());
    Player updatedPlayerTwo = playerService.getPlayerById(playerTwo.playerId());
    Player updatedPlayerThree = playerService.getPlayerById(playerThree.playerId());
    Player updatedPlayerFour = playerService.getPlayerById(playerFour.playerId());

    assertThat(updatedPlayerOne.getEloRating()).isEqualTo(new BigDecimal("1215.00"));
    assertThat(updatedPlayerTwo.getEloRating()).isEqualTo(new BigDecimal("1185.00"));
    assertThat(updatedPlayerThree.getEloRating()).isEqualTo(new BigDecimal("1215.00"));
    assertThat(updatedPlayerFour.getEloRating()).isEqualTo(new BigDecimal("1185.00"));

    assertThat(matchRepository.existsById(secondMatch.matchId())).isFalse();
    assertThat(matchRepository.existsById(firstMatch.matchId())).isTrue();
    assertThat(matchRepository.existsById(thirdMatch.matchId())).isTrue();
  }

  @Test
  public void testPlayerIdInMatchDoesNotChange() {
    PlayerResponse firstPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(WINNER_NICKNAME).build());
    PlayerResponse secondPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(LOSER_NICKNAME).build());

    Long firstPlayerIdBefore = firstPlayer.playerId();
    Long secondPlayerIdBefore = secondPlayer.playerId();

    matchService.createMatch(
        CreateMatchRequest.builder().winnerId(firstPlayer.playerId()).loserId(secondPlayer.playerId()).build());

    Player updatedFirstPlayer = playerService.getPlayerById(firstPlayer.playerId());
    Player updatedSecondPlayer = playerService.getPlayerById(secondPlayer.playerId());

    assertThat(updatedFirstPlayer.getPlayerId()).isEqualTo(firstPlayerIdBefore);
    assertThat(updatedSecondPlayer.getPlayerId()).isEqualTo(secondPlayerIdBefore);
  }

  @Test
  public void testMatchDoesNotModifyExistingInfo() {
    PlayerResponse firstPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(WINNER_NICKNAME).build());
    PlayerResponse secondPlayer =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname(LOSER_NICKNAME).build());

    Player firstPlayerBeforeMatch = playerService.getPlayerById(firstPlayer.playerId());
    Player secondPlayerBeforeMatch = playerService.getPlayerById(secondPlayer.playerId());

    CreateMatchRequest matchRequest =
        CreateMatchRequest.builder().winnerId(firstPlayer.playerId()).loserId(secondPlayer.playerId()).build();

    matchService.createMatch(matchRequest);

    Player firstPlayerAfterMatch = playerService.getPlayerById(firstPlayer.playerId());
    Player secondPlayerAfterMatch = playerService.getPlayerById(secondPlayer.playerId());

    assertThat(firstPlayerAfterMatch.getNickname()).isEqualTo(firstPlayerBeforeMatch.getNickname());
    assertThat(secondPlayerAfterMatch.getNickname()).isEqualTo(secondPlayerBeforeMatch.getNickname());
  }
}
