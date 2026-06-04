package com.emt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.mapper.TournamentMapper;
import com.emt.metrics.BusinessMetrics;
import com.emt.model.exception.TournamentCreationException;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import com.emt.repository.PlayerRepository;
import com.emt.repository.TournamentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

  private static final String FIRST_PLAYER = "FirstPlayer";
  private static final String SECOND_PLAYER = "SecondPlayer";

  @Mock private PlayerRepository playerRepository;
  @Mock private TournamentRepository tournamentRepository;
  @Spy private TournamentMapper tournamentMapper;
  @Mock private BusinessMetrics businessMetrics;
  @InjectMocks private TournamentService tournamentService;

  @Test
  void createTournament_withManualSeeding_ShouldPersistSeededRoster() {
    Player firstPlayer = player(1L, FIRST_PLAYER);
    Player secondPlayer = player(2L, SECOND_PLAYER);
    CreateTournamentRequest request = tournamentRequest(List.of(1L, 2L));

    given(playerRepository.findAllById(List.of(1L, 2L)))
        .willReturn(List.of(secondPlayer, firstPlayer));
    given(tournamentRepository.save(org.mockito.ArgumentMatchers.any(Tournament.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    TournamentResponse response = tournamentService.createTournament(request);

    ArgumentCaptor<Tournament> tournamentCaptor = ArgumentCaptor.forClass(Tournament.class);
    verify(tournamentRepository).save(tournamentCaptor.capture());
    Tournament savedTournament = tournamentCaptor.getValue();

    assertThat(savedTournament.getName()).isEqualTo("Summer Finals");
    assertThat(savedTournament.getParticipants()).hasSize(2);
    assertThat(savedTournament.getParticipants())
        .extracting(participant -> participant.getPlayer().getNickname())
        .containsExactly(FIRST_PLAYER, SECOND_PLAYER);
    assertThat(response.participants())
        .extracting(participant -> participant.seedNumber())
        .containsExactly(1, 2);
    verify(businessMetrics).recordTournamentCreated();
  }

  @Test
  void createTournament_withUnsupportedPlayerCount_ShouldThrowException() {
    assertThatThrownBy(
            () ->
                tournamentService.createTournament(
                    CreateTournamentRequest.builder()
                        .name("Bad size")
                        .playerCount(3)
                        .seedingMode(SeedingMode.MANUAL)
                        .gameFormat(GameFormat.BO1)
                        .winningPoints(11)
                        .bracketType(BracketType.SINGLE_ELIMINATION)
                        .playerIds(List.of(1L, 2L, 3L))
                        .build()))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("Unsupported player count");
  }

  @Test
  void createTournament_withDuplicatePlayers_ShouldThrowException() {
    assertThatThrownBy(() -> tournamentService.createTournament(tournamentRequest(List.of(1L, 1L))))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("only be selected once");
  }

  @Test
  void createTournament_withUnknownPlayer_ShouldThrowException() {
    given(playerRepository.findAllById(List.of(1L, 2L)))
        .willReturn(List.of(player(1L, FIRST_PLAYER)));

    assertThatThrownBy(() -> tournamentService.createTournament(tournamentRequest(List.of(1L, 2L))))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("Players not found with ids: [2]");
  }

  @Test
  void createTournament_withRosterSizeMismatch_ShouldThrowException() {
    assertThatThrownBy(
            () ->
                tournamentService.createTournament(
                    CreateTournamentRequest.builder()
                        .name("Bad roster")
                        .playerCount(4)
                        .seedingMode(SeedingMode.MANUAL)
                        .gameFormat(GameFormat.BO3)
                        .winningPoints(11)
                        .bracketType(BracketType.ROUND_ROBIN)
                        .playerIds(List.of(1L, 2L))
                        .build()))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("Expected 4 players");
  }

  @Test
  void getAllTournaments_ShouldReturnTournamentResponses() {
    Player firstPlayer = player(1L, FIRST_PLAYER);
    Tournament tournament =
        Tournament.builder()
            .tournamentId(10L)
            .name("Stored Tournament")
            .playerCount(2)
            .seedingMode(SeedingMode.MANUAL)
            .gameFormat(GameFormat.BO1)
            .winningPoints(21)
            .bracketType(BracketType.ROUND_ROBIN)
            .createdAt(Instant.now())
            .build();
    tournament
        .getParticipants()
        .add(
            com.emt.entity.TournamentParticipant.builder()
                .tournament(tournament)
                .player(firstPlayer)
                .seedNumber(1)
                .build());

    given(tournamentRepository.findAllWithParticipants()).willReturn(List.of(tournament));

    List<TournamentResponse> tournaments = tournamentService.getAllTournaments();

    assertThat(tournaments).hasSize(1);
    assertThat(tournaments.get(0).name()).isEqualTo("Stored Tournament");
    assertThat(tournaments.get(0).participants()).hasSize(1);
  }

  private CreateTournamentRequest tournamentRequest(List<Long> playerIds) {
    return CreateTournamentRequest.builder()
        .name(" Summer Finals ")
        .playerCount(2)
        .seedingMode(SeedingMode.MANUAL)
        .gameFormat(GameFormat.BO3)
        .winningPoints(11)
        .bracketType(BracketType.SINGLE_ELIMINATION)
        .playerIds(playerIds)
        .build();
  }

  private Player player(Long playerId, String nickname) {
    return new Player(playerId, nickname, new BigDecimal("1200"), Instant.now());
  }
}
