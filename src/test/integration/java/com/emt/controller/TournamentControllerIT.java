package com.emt.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.emt.ITBase;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.TournamentMatchResponse;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import com.emt.model.tournament.TournamentStatus;
import com.emt.repository.TournamentRepository;
import com.emt.service.PlayerService;
import com.emt.service.TournamentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RequiredArgsConstructor
class TournamentControllerIT extends ITBase {

  private static final String TOURNAMENTS_PATH = "/tournaments";
  private static final String TOURNAMENTS_VIEW = "tournaments";
  private static final String START_PATH_SUFFIX = "/start";
  private static final String MESSAGE_ATTRIBUTE = "message";

  private final MockMvc mockMvc;
  private final PlayerService playerService;
  private final TournamentRepository tournamentRepository;
  private final TournamentService tournamentService;

  @Test
  void getTournaments_ShouldRenderTournamentSetupPage() throws Exception {
    mockMvc
        .perform(get(TOURNAMENTS_PATH))
        .andExpect(status().isOk())
        .andExpect(view().name(TOURNAMENTS_VIEW))
        .andExpect(model().attributeExists("players"))
        .andExpect(model().attributeExists("tournaments"))
        .andExpect(model().attributeExists("tournamentRequest"))
        .andExpect(model().attributeExists("playerCounts"))
        .andExpect(model().attributeExists("seedingModes"))
        .andExpect(model().attributeExists("gameFormats"))
        .andExpect(model().attributeExists("bracketTypes"));
  }

  @Test
  void createTournament_withValidRequest_ShouldCreateTournament() throws Exception {
    Long firstPlayerId = createPlayer("BracketOne");
    Long secondPlayerId = createPlayer("BracketTwo");

    mockMvc
        .perform(
            post(TOURNAMENTS_PATH)
                .param("name", "Friday Finals")
                .param("playerCount", "2")
                .param("seedingMode", "MANUAL")
                .param("gameFormat", "BO3")
                .param("winningPoints", "11")
                .param("bracketType", "SINGLE_ELIMINATION")
                .param("playerIds", String.valueOf(firstPlayerId), String.valueOf(secondPlayerId)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(flash().attribute(MESSAGE_ATTRIBUTE, "Tournament created successfully!"));

    List<TournamentResponse> tournaments = tournamentService.getAllTournaments();

    assertThat(tournaments).hasSize(1);
    assertThat(tournaments.get(0).name()).isEqualTo("Friday Finals");
    assertThat(tournaments.get(0).participants())
        .extracting(participant -> participant.nickname())
        .containsExactly("BracketOne", "BracketTwo");
  }

  @Test
  @SuppressWarnings("unchecked")
  void getTournaments_withExistingTournament_ShouldExposeSeededGrid() throws Exception {
    Long firstPlayerId = createPlayer("GridOne");
    Long secondPlayerId = createPlayer("GridTwo");
    tournamentService.createTournament(
        CreateTournamentRequest.builder()
            .name("Grid Cup")
            .playerCount(2)
            .seedingMode(SeedingMode.MANUAL)
            .gameFormat(GameFormat.BO1)
            .winningPoints(21)
            .bracketType(BracketType.ROUND_ROBIN)
            .playerIds(List.of(firstPlayerId, secondPlayerId))
            .build());

    MvcResult result =
        mockMvc
            .perform(get(TOURNAMENTS_PATH))
            .andExpect(status().isOk())
            .andExpect(view().name(TOURNAMENTS_VIEW))
            .andReturn();

    List<TournamentResponse> tournaments =
        (List<TournamentResponse>) result.getModelAndView().getModel().get("tournaments");

    assertThat(tournaments).hasSize(1);
    assertThat(tournaments.get(0).participants())
        .extracting(participant -> participant.seedNumber())
        .containsExactly(1, 2);
  }


  @Test
  void tournamentEngine_ShouldStartAndRecordFinalMatch() throws Exception {
    Long firstPlayerId = createPlayer("EngineOne");
    Long secondPlayerId = createPlayer("EngineTwo");
    TournamentResponse tournament =
        tournamentService.createTournament(
            CreateTournamentRequest.builder()
                .name("Engine Cup")
                .playerCount(2)
                .seedingMode(SeedingMode.MANUAL)
                .gameFormat(GameFormat.BO1)
                .winningPoints(11)
                .bracketType(BracketType.SINGLE_ELIMINATION)
                .playerIds(List.of(firstPlayerId, secondPlayerId))
                .build());

    mockMvc
        .perform(post(TOURNAMENTS_PATH + "/" + tournament.tournamentId() + START_PATH_SUFFIX))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(flash().attribute(MESSAGE_ATTRIBUTE, "Tournament started successfully!"));

    TournamentResponse startedTournament = tournamentService.getAllTournaments().get(0);
    Long tournamentMatchId = startedTournament.matches().get(0).tournamentMatchId();

    mockMvc
        .perform(
            post(TOURNAMENTS_PATH + "/matches/" + tournamentMatchId + "/report")
                .param("winnerId", String.valueOf(firstPlayerId)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(flash().attribute(MESSAGE_ATTRIBUTE, "Tournament match recorded successfully!"));

    TournamentResponse completedTournament = tournamentService.getAllTournaments().get(0);

    assertThat(completedTournament.status()).isEqualTo(TournamentStatus.COMPLETED);
    assertThat(completedTournament.winnerNickname()).isEqualTo("EngineOne");
    assertThat(completedTournament.matches()).hasSize(1);
  }


  @Test
  void roundRobinTournamentEngine_ShouldCompleteViaHttpFlow() throws Exception {
    Long firstPlayerId = createPlayer("RobinEngineOne");
    Long secondPlayerId = createPlayer("RobinEngineTwo");
    Long thirdPlayerId = createPlayer("RobinEngineThree");
    Long fourthPlayerId = createPlayer("RobinEngineFour");
    TournamentResponse tournament =
        tournamentService.createTournament(
            CreateTournamentRequest.builder()
                .name("Round Robin Engine Cup")
                .playerCount(4)
                .seedingMode(SeedingMode.MANUAL)
                .gameFormat(GameFormat.BO1)
                .winningPoints(11)
                .bracketType(BracketType.ROUND_ROBIN)
                .playerIds(List.of(firstPlayerId, secondPlayerId, thirdPlayerId, fourthPlayerId))
                .build());

    mockMvc
        .perform(post(TOURNAMENTS_PATH + "/" + tournament.tournamentId() + START_PATH_SUFFIX))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(flash().attribute(MESSAGE_ATTRIBUTE, "Tournament started successfully!"));

    TournamentResponse startedTournament = tournamentService.getAllTournaments().get(0);

    assertThat(startedTournament.matches()).hasSize(6);

    for (TournamentMatchResponse match : startedTournament.matches()) {
      Long winnerId = roundRobinWinnerFor(match, firstPlayerId);
      mockMvc
          .perform(
              post(TOURNAMENTS_PATH + "/matches/" + match.tournamentMatchId() + "/report")
                  .param("winnerId", String.valueOf(winnerId)))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl(TOURNAMENTS_PATH))
          .andExpect(flash().attribute(MESSAGE_ATTRIBUTE, "Tournament match recorded successfully!"));
    }

    TournamentResponse completedTournament = tournamentService.getAllTournaments().get(0);

    assertThat(completedTournament.status()).isEqualTo(TournamentStatus.COMPLETED);
    assertThat(completedTournament.winnerNickname()).isEqualTo("RobinEngineOne");
  }

  @Test
  void startTournament_withNonDraftTournament_ShouldRedirectWithError() throws Exception {
    Long firstPlayerId = createPlayer("NonDraftOne");
    Long secondPlayerId = createPlayer("NonDraftTwo");
    TournamentResponse tournament =
        tournamentService.createTournament(
            CreateTournamentRequest.builder()
                .name("Non Draft Cup")
                .playerCount(2)
                .seedingMode(SeedingMode.MANUAL)
                .gameFormat(GameFormat.BO1)
                .winningPoints(11)
                .bracketType(BracketType.SINGLE_ELIMINATION)
                .playerIds(List.of(firstPlayerId, secondPlayerId))
                .build());

    mockMvc
        .perform(post(TOURNAMENTS_PATH + "/" + tournament.tournamentId() + START_PATH_SUFFIX))
        .andExpect(status().is3xxRedirection());

    mockMvc
        .perform(post(TOURNAMENTS_PATH + "/" + tournament.tournamentId() + START_PATH_SUFFIX))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(
            flash()
                .attribute(
                    "error",
                    "Tournament creation failed: Only draft tournaments can be started."));
  }

  @Test
  void reportTournamentMatch_withInvalidWinner_ShouldRedirectWithError() throws Exception {
    Long firstPlayerId = createPlayer("InvalidWinnerOne");
    Long secondPlayerId = createPlayer("InvalidWinnerTwo");
    Long invalidWinnerId = createPlayer("InvalidWinnerThree");
    TournamentResponse tournament =
        tournamentService.createTournament(
            CreateTournamentRequest.builder()
                .name("Invalid Winner Cup")
                .playerCount(2)
                .seedingMode(SeedingMode.MANUAL)
                .gameFormat(GameFormat.BO1)
                .winningPoints(11)
                .bracketType(BracketType.SINGLE_ELIMINATION)
                .playerIds(List.of(firstPlayerId, secondPlayerId))
                .build());

    mockMvc
        .perform(post(TOURNAMENTS_PATH + "/" + tournament.tournamentId() + START_PATH_SUFFIX))
        .andExpect(status().is3xxRedirection());

    TournamentResponse startedTournament = tournamentService.getAllTournaments().get(0);
    Long tournamentMatchId = startedTournament.matches().get(0).tournamentMatchId();

    mockMvc
        .perform(
            post(TOURNAMENTS_PATH + "/matches/" + tournamentMatchId + "/report")
                .param("winnerId", String.valueOf(invalidWinnerId)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(
            flash()
                .attribute(
                    "error",
                    "Tournament creation failed: Winner must be one of the tournament match players."));
  }

  @Test
  void createTournament_withRosterMismatch_ShouldRedirectWithError() throws Exception {
    Long firstPlayerId = createPlayer("MismatchOne");
    createPlayer("MismatchTwo");

    mockMvc
        .perform(
            post(TOURNAMENTS_PATH)
                .param("name", "Mismatch Cup")
                .param("playerCount", "2")
                .param("seedingMode", "MANUAL")
                .param("gameFormat", "BO1")
                .param("winningPoints", "11")
                .param("bracketType", "SINGLE_ELIMINATION")
                .param("playerIds", String.valueOf(firstPlayerId)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(TOURNAMENTS_PATH))
        .andExpect(flash().attributeExists("error"));

    assertThat(tournamentRepository.findAll()).isEmpty();
  }

  private Long roundRobinWinnerFor(TournamentMatchResponse match, Long preferredWinnerId) {
    if (match.firstPlayerId().equals(preferredWinnerId)
        || match.secondPlayerId().equals(preferredWinnerId)) {
      return preferredWinnerId;
    }
    return match.firstPlayerId();
  }

  private Long createPlayer(String nickname) {
    return playerService
        .createPlayer(CreatePlayerRequest.builder().nickname(nickname).build())
        .playerId();
  }
}
