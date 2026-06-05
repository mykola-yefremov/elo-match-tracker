package com.emt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.entity.TournamentParticipant;
import com.emt.mapper.TournamentMapper;
import com.emt.metrics.BusinessMetrics;
import com.emt.model.exception.TournamentCreationException;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import com.emt.model.tournament.TournamentMatchStatus;
import com.emt.model.tournament.TournamentStatus;
import com.emt.repository.PlayerRepository;
import com.emt.repository.TournamentMatchRepository;
import com.emt.repository.TournamentRepository;
import com.emt.service.tournament.RoundRobinBracketStrategy;
import com.emt.service.tournament.SingleEliminationBracketStrategy;
import com.emt.service.tournament.TournamentMatchFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

  private static final String FIRST_PLAYER = "FirstPlayer";
  private static final String SECOND_PLAYER = "SecondPlayer";

  @Mock private PlayerRepository playerRepository;
  @Mock private TournamentRepository tournamentRepository;
  @Mock private TournamentMatchRepository tournamentMatchRepository;
  @Mock private MatchService matchService;
  @Mock private BusinessMetrics businessMetrics;

  private TournamentService tournamentService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    TournamentMapper tournamentMapper = new TournamentMapper();
    TournamentMatchFactory tournamentMatchFactory = new TournamentMatchFactory(clock);

    tournamentService =
        new TournamentService(
            playerRepository,
            tournamentRepository,
            tournamentMatchRepository,
            tournamentMapper,
            matchService,
            businessMetrics,
            clock,
            List.of(
                new SingleEliminationBracketStrategy(clock, tournamentMatchFactory),
                new RoundRobinBracketStrategy(clock, tournamentMatchFactory)));
  }

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
  void createTournament_withRandomSeeding_ShouldRecordMetricsAndPersistSeededRoster() {
    Player firstPlayer = player(1L, FIRST_PLAYER);
    Player secondPlayer = player(2L, SECOND_PLAYER);
    CreateTournamentRequest request = tournamentRequest(List.of(1L, 2L), SeedingMode.RANDOM);

    given(playerRepository.findAllById(List.of(1L, 2L)))
        .willReturn(List.of(firstPlayer, secondPlayer));
    given(tournamentRepository.save(org.mockito.ArgumentMatchers.any(Tournament.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    TournamentResponse response = tournamentService.createTournament(request);

    assertThat(response.participants()).hasSize(2);
    assertThat(response.participants())
        .extracting(participant -> participant.seedNumber())
        .containsExactly(1, 2);
    assertThat(response.participants())
        .extracting(participant -> participant.nickname())
        .containsExactlyInAnyOrder(FIRST_PLAYER, SECOND_PLAYER);
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
  void startTournament_withSingleElimination_ShouldGenerateSeededFirstRound() {
    Player firstPlayer = player(1L, "SeedOne");
    Player secondPlayer = player(2L, "SeedTwo");
    Player thirdPlayer = player(3L, "SeedThree");
    Player fourthPlayer = player(4L, "SeedFour");
    Tournament tournament =
        tournament(
            10L,
            4,
            BracketType.SINGLE_ELIMINATION,
            List.of(firstPlayer, secondPlayer, thirdPlayer, fourthPlayer));

    given(tournamentRepository.findById(10L)).willReturn(Optional.of(tournament));
    given(tournamentRepository.save(any(Tournament.class))).willAnswer(invocation -> invocation.getArgument(0));

    TournamentResponse response = tournamentService.startTournament(10L);

    assertThat(response.status()).isEqualTo(TournamentStatus.ACTIVE);
    assertThat(response.matches()).hasSize(2);
    assertThat(response.matches())
        .extracting(match -> match.firstPlayerNickname() + " vs " + match.secondPlayerNickname())
        .containsExactly("SeedOne vs SeedFour", "SeedTwo vs SeedThree");
  }

  @Test
  void startTournament_withRoundRobin_ShouldGenerateEveryPairAcrossRounds() {
    Player firstPlayer = player(1L, "RobinOne");
    Player secondPlayer = player(2L, "RobinTwo");
    Player thirdPlayer = player(3L, "RobinThree");
    Player fourthPlayer = player(4L, "RobinFour");
    Tournament tournament =
        tournament(
            11L,
            4,
            BracketType.ROUND_ROBIN,
            List.of(firstPlayer, secondPlayer, thirdPlayer, fourthPlayer));

    given(tournamentRepository.findById(11L)).willReturn(Optional.of(tournament));
    given(tournamentRepository.save(any(Tournament.class))).willAnswer(invocation -> invocation.getArgument(0));

    TournamentResponse response = tournamentService.startTournament(11L);

    assertThat(response.status()).isEqualTo(TournamentStatus.ACTIVE);
    assertThat(response.matches()).hasSize(6);
    assertThat(response.matches())
        .extracting(match -> match.roundNumber())
        .containsExactly(1, 1, 2, 2, 3, 3);
  }


  @Test
  void startTournament_withNonDraftStatus_ShouldThrowException() {
    Tournament tournament =
        tournament(
            13L,
            2,
            BracketType.SINGLE_ELIMINATION,
            List.of(player(1L, FIRST_PLAYER), player(2L, SECOND_PLAYER)));
    tournament.setStatus(TournamentStatus.ACTIVE);

    given(tournamentRepository.findById(13L)).willReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.startTournament(13L))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("Only draft tournaments can be started.");
  }

  @Test
  void startTournament_withExistingMatches_ShouldThrowException() {
    Player firstPlayer = player(1L, FIRST_PLAYER);
    Player secondPlayer = player(2L, SECOND_PLAYER);
    Tournament tournament =
        tournament(14L, 2, BracketType.SINGLE_ELIMINATION, List.of(firstPlayer, secondPlayer));
    tournament.getMatches().add(tournamentMatch(21L, tournament, firstPlayer, secondPlayer));

    given(tournamentRepository.findById(14L)).willReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.startTournament(14L))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("Tournament bracket has already been generated.");
  }

  @Test
  void startTournament_withIncompleteRoster_ShouldThrowException() {
    Tournament tournament =
        tournament(
            15L,
            4,
            BracketType.SINGLE_ELIMINATION,
            List.of(player(1L, FIRST_PLAYER), player(2L, SECOND_PLAYER)));

    given(tournamentRepository.findById(15L)).willReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.startTournament(15L))
        .isInstanceOf(TournamentCreationException.class)
        .hasMessageContaining("Tournament roster is incomplete.");
  }

  @Test
  void reportTournamentMatchResult_withFinalSingleEliminationMatch_ShouldCompleteTournament() {
    Player firstPlayer = player(1L, FIRST_PLAYER);
    Player secondPlayer = player(2L, SECOND_PLAYER);
    Tournament tournament = tournament(12L, 2, BracketType.SINGLE_ELIMINATION, List.of(firstPlayer, secondPlayer));
    tournament.setStatus(TournamentStatus.ACTIVE);
    TournamentMatch tournamentMatch = tournamentMatch(20L, tournament, firstPlayer, secondPlayer);
    tournament.getMatches().add(tournamentMatch);

    given(tournamentMatchRepository.findWithPlayersByTournamentMatchId(20L))
        .willReturn(Optional.of(tournamentMatch));
    given(tournamentRepository.save(any(Tournament.class))).willAnswer(invocation -> invocation.getArgument(0));

    TournamentResponse response = tournamentService.reportTournamentMatchResult(20L, 1L);

    assertThat(response.status()).isEqualTo(TournamentStatus.COMPLETED);
    assertThat(response.winnerNickname()).isEqualTo(FIRST_PLAYER);
    assertThat(response.matches().get(0).status()).isEqualTo(TournamentMatchStatus.COMPLETED);
    verify(matchService).createMatch(any());
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
            TournamentParticipant.builder()
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

  private Tournament tournament(
      Long tournamentId, Integer playerCount, BracketType bracketType, List<Player> players) {
    Tournament tournament =
        Tournament.builder()
            .tournamentId(tournamentId)
            .name("Stored Tournament")
            .playerCount(playerCount)
            .seedingMode(SeedingMode.MANUAL)
            .gameFormat(GameFormat.BO1)
            .winningPoints(21)
            .bracketType(bracketType)
            .createdAt(Instant.now())
            .build();

    for (int i = 0; i < players.size(); i++) {
      tournament
          .getParticipants()
          .add(
              TournamentParticipant.builder()
                  .tournament(tournament)
                  .player(players.get(i))
                  .seedNumber(i + 1)
                  .build());
    }
    return tournament;
  }

  private TournamentMatch tournamentMatch(
      Long tournamentMatchId, Tournament tournament, Player firstPlayer, Player secondPlayer) {
    return TournamentMatch.builder()
        .tournamentMatchId(tournamentMatchId)
        .tournament(tournament)
        .roundNumber(1)
        .matchNumber(1)
        .firstPlayer(firstPlayer)
        .secondPlayer(secondPlayer)
        .status(TournamentMatchStatus.PENDING)
        .createdAt(Instant.now())
        .build();
  }

  private CreateTournamentRequest tournamentRequest(List<Long> playerIds) {
    return tournamentRequest(playerIds, SeedingMode.MANUAL);
  }

  private CreateTournamentRequest tournamentRequest(
      List<Long> playerIds, SeedingMode seedingMode) {
    return CreateTournamentRequest.builder()
        .name(" Summer Finals ")
        .playerCount(2)
        .seedingMode(seedingMode)
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
