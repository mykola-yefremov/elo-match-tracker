package com.emt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.emt.entity.Match;
import com.emt.entity.Player;
import com.emt.mapper.MatchMapper;
import com.emt.mapper.PlayerMapper;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.PlayerProfileResponse;
import com.emt.model.response.PlayerResponse;
import com.emt.repository.MatchRepository;
import com.emt.repository.PlayerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

  private static final String NICKNAME = "topOneDeadlocker";
  private static final BigDecimal DEFAULT_RATING = new BigDecimal("1200");

  @Mock private PlayerRepository playerRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private PlayerMapper playerMapper;
  @Mock private MatchMapper matchMapper;
  @InjectMocks private PlayerService playerService;

  @Test
  void createPlayer_WhenPlayerDoesNotExist_ShouldCreateNewPlayer() {
    CreatePlayerRequest request = CreatePlayerRequest.builder().nickname(NICKNAME).build();
    Player player = new Player(1L, NICKNAME, new BigDecimal("2500"), Instant.now());
    PlayerResponse expectedResponse =
        PlayerResponse.builder()
            .playerId(1L)
            .nickname(NICKNAME)
            .eloRating(new BigDecimal("2500"))
            .registeredAt(player.getRegisteredAt())
            .build();

    given(playerRepository.existsByNickname(request.nickname())).willReturn(false);
    given(playerMapper.mapToEntity(request)).willReturn(player);
    given(playerRepository.save(player)).willReturn(player);
    given(playerMapper.mapToResponse(player)).willReturn(expectedResponse);

    PlayerResponse actualResponse = playerService.createPlayer(request);

    assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    verify(playerRepository).save(player);
  }

  @Test
  void createPlayer_WhenPlayerAlreadyExists_ShouldThrowException() {
    CreatePlayerRequest request = CreatePlayerRequest.builder().nickname(NICKNAME).build();

    given(playerRepository.existsByNickname(request.nickname())).willReturn(true);

    assertThatThrownBy(() -> playerService.createPlayer(request))
        .isInstanceOf(PlayerAlreadyExistsException.class)
        .hasMessageContaining("Player with nickname " + NICKNAME + " already exists.");
  }

  @Test
  void getAllPlayers_WhenPlayersExist_ShouldReturnPlayersSortedByRatingDescending() {
    Instant registeredAt = Instant.now();
    Player strongerPlayer = new Player(2L, "strongerPlayer", new BigDecimal("1400"), registeredAt);
    Player weakerPlayer = new Player(1L, "weakerPlayer", DEFAULT_RATING, registeredAt);
    PlayerResponse strongerResponse =
        new PlayerResponse(2L, "strongerPlayer", new BigDecimal("1400"), registeredAt);
    PlayerResponse weakerResponse =
        new PlayerResponse(1L, "weakerPlayer", DEFAULT_RATING, registeredAt);

    given(playerRepository.findAll()).willReturn(List.of(weakerPlayer, strongerPlayer));
    given(playerMapper.mapToResponse(strongerPlayer)).willReturn(strongerResponse);
    given(playerMapper.mapToResponse(weakerPlayer)).willReturn(weakerResponse);

    List<PlayerResponse> players = playerService.getAllPlayers();

    assertThat(players).containsExactly(strongerResponse, weakerResponse);
  }

  @Test
  void getPlayersForRatingUpdate_WhenBothPlayersExist_ShouldReturnLockedPlayers() {
    Player firstPlayer = new Player(1L, "firstPlayer", DEFAULT_RATING, Instant.now());
    Player secondPlayer = new Player(2L, "secondPlayer", DEFAULT_RATING, Instant.now());
    given(playerRepository.findPlayersForUpdate(List.of(1L, 2L)))
        .willReturn(List.of(firstPlayer, secondPlayer));

    List<Player> players = playerService.getPlayersForRatingUpdate(1L, 2L);

    assertThat(players).containsExactly(firstPlayer, secondPlayer);
  }

  @Test
  void getPlayersForRatingUpdate_WhenPlayerIsMissing_ShouldThrowException() {
    Player firstPlayer = new Player(1L, "firstPlayer", DEFAULT_RATING, Instant.now());
    given(playerRepository.findPlayersForUpdate(List.of(1L, 2L))).willReturn(List.of(firstPlayer));

    assertThatThrownBy(() -> playerService.getPlayersForRatingUpdate(1L, 2L))
        .isInstanceOf(PlayerNotFoundException.class)
        .hasMessageContaining("Player with id 2 not found");
  }

  @Test
  void getPlayerById_WhenPlayerDoesNotExist_ShouldThrowException() {
    given(playerRepository.findById(404L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playerService.getPlayerById(404L))
        .isInstanceOf(PlayerNotFoundException.class)
        .hasMessageContaining("Player with id 404 not found");
  }

  @Test
  void getPlayerProfile_WhenPlayerHasMatches_ShouldReturnStatsAndHistory() {
    Instant registeredAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant matchTime = Instant.parse("2026-01-02T00:00:00Z");
    Player player = new Player(1L, NICKNAME, new BigDecimal("1215.00"), registeredAt);
    Player opponent = new Player(2L, "opponent", DEFAULT_RATING, registeredAt);
    Match match = new Match(10L, player, opponent, new BigDecimal("15.00"), matchTime);
    MatchResponse matchResponse =
        MatchResponse.builder()
            .matchId(10L)
            .winnerId(1L)
            .winnerName(NICKNAME)
            .loserId(2L)
            .loserName("opponent")
            .winnerRatingChange(new BigDecimal("15.00"))
            .createdAt(matchTime)
            .build();
    PlayerResponse playerResponse =
        new PlayerResponse(1L, NICKNAME, new BigDecimal("1215.00"), registeredAt);

    given(playerRepository.findById(1L)).willReturn(Optional.of(player));
    given(matchRepository.findMatchesByPlayer(1L)).willReturn(List.of(match));
    given(matchMapper.mapToResponse(match)).willReturn(matchResponse);
    given(playerMapper.mapToResponse(player)).willReturn(playerResponse);
    given(matchRepository.countByWinner_PlayerId(1L)).willReturn(1);
    given(matchRepository.countByLoser_PlayerId(1L)).willReturn(0);

    PlayerProfileResponse profile = playerService.getPlayerProfile(1L);

    assertThat(profile.player()).isEqualTo(playerResponse);
    assertThat(profile.stats().wins()).isEqualTo(1);
    assertThat(profile.stats().winRate()).isEqualByComparingTo("100.00");
    assertThat(profile.recentMatches()).containsExactly(matchResponse);
    assertThat(profile.ratingHistory()).hasSize(2);
    assertThat(profile.ratingHistory().get(1).rating()).isEqualByComparingTo("1215.00");
  }
}
