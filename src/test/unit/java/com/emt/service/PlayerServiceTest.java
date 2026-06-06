package com.emt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.emt.entity.Player;
import com.emt.mapper.PlayerMapper;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.PlayerResponse;
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
  @Mock private PlayerMapper playerMapper;
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
}
