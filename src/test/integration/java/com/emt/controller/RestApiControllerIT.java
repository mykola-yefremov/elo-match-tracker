package com.emt.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emt.ITBase;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import com.emt.model.tournament.TournamentStatus;
import com.emt.service.PlayerService;
import com.emt.service.TournamentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RequiredArgsConstructor
class RestApiControllerIT extends ITBase {

  private static final String PLAYERS_API = "/api/v1/players";
  private static final String MATCHES_API = "/api/v1/matches";
  private static final String TOURNAMENTS_API = "/api/v1/tournaments";
  private static final String STATUS_JSON_PATH = "$.status";

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final PlayerService playerService;
  private final TournamentService tournamentService;

  @Test
  void playersApi_ShouldCreateReadAndPaginatePlayers() throws Exception {
    mockMvc
        .perform(
            post(PLAYERS_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nickname":"ApiPlayerOne"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.playerId").isNumber())
        .andExpect(jsonPath("$.nickname").value("ApiPlayerOne"))
        .andExpect(jsonPath("$.eloRating").value(1200));

    Long playerId = createPlayer("ApiPlayerTwo");

    mockMvc
        .perform(get(PLAYERS_API + "/" + playerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("ApiPlayerTwo"));

    mockMvc
        .perform(get(PLAYERS_API).param("page", "0").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(false));
  }

  @Test
  void playersApi_withInvalidRequest_ShouldReturnJsonError() throws Exception {
    mockMvc
        .perform(
            post(PLAYERS_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nickname":""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath(STATUS_JSON_PATH).value(400))
        .andExpect(jsonPath("$.path").value(PLAYERS_API))
        .andExpect(jsonPath("$.validationErrors.nickname").exists());
  }

  @Test
  void matchesApi_ShouldCreateFilterAndCancelMatch() throws Exception {
    Long winnerId = createPlayer("ApiWinner");
    Long loserId = createPlayer("ApiLoser");

    MvcResult result =
        mockMvc
            .perform(
                post(MATCHES_API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"winnerId":%d,"loserId":%d}
                            """
                            .formatted(winnerId, loserId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.winnerName").value("ApiWinner"))
            .andExpect(jsonPath("$.loserName").value("ApiLoser"))
            .andReturn();

    MatchResponse match = objectMapper.readValue(result.getResponse().getContentAsString(), MatchResponse.class);

    mockMvc
        .perform(get(MATCHES_API).param("playerId", String.valueOf(winnerId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].winnerName").value("ApiWinner"));

    mockMvc.perform(delete(MATCHES_API + "/" + match.matchId())).andExpect(status().isNoContent());

    mockMvc
        .perform(get(MATCHES_API).param("playerId", String.valueOf(winnerId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void matchesApi_withIdenticalPlayers_ShouldReturnJsonError() throws Exception {
    Long playerId = createPlayer("ApiSamePlayer");

    mockMvc
        .perform(
            post(MATCHES_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"winnerId":%d,"loserId":%d}
                        """
                        .formatted(playerId, playerId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("identical players")));
  }

  @Test
  void tournamentsApi_ShouldCreateStartAndReportResult() throws Exception {
    Long firstPlayerId = createPlayer("ApiSeedOne");
    Long secondPlayerId = createPlayer("ApiSeedTwo");

    MvcResult createResult =
        mockMvc
            .perform(
                post(TOURNAMENTS_API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                              "name":"Api Cup",
                              "playerCount":2,
                              "seedingMode":"MANUAL",
                              "gameFormat":"BO1",
                              "winningPoints":11,
                              "bracketType":"SINGLE_ELIMINATION",
                              "playerIds":[%d,%d]
                            }
                            """
                            .formatted(firstPlayerId, secondPlayerId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath(STATUS_JSON_PATH).value("DRAFT"))
            .andReturn();

    TournamentResponse createdTournament =
        objectMapper.readValue(createResult.getResponse().getContentAsString(), TournamentResponse.class);

    MvcResult startResult =
        mockMvc
            .perform(post(TOURNAMENTS_API + "/" + createdTournament.tournamentId() + "/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(STATUS_JSON_PATH).value("ACTIVE"))
            .andExpect(jsonPath("$.matches.length()").value(1))
            .andReturn();

    TournamentResponse startedTournament =
        objectMapper.readValue(startResult.getResponse().getContentAsString(), TournamentResponse.class);
    Long tournamentMatchId = startedTournament.matches().get(0).tournamentMatchId();

    mockMvc
        .perform(
            post(TOURNAMENTS_API + "/matches/" + tournamentMatchId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"winnerId":%d}
                    """.formatted(firstPlayerId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath(STATUS_JSON_PATH).value(TournamentStatus.COMPLETED.name()))
        .andExpect(jsonPath("$.winnerNickname").value("ApiSeedOne"));
  }


  @Test
  void tournamentsApi_withMissingTournament_ShouldReturnNotFound() throws Exception {
    mockMvc
        .perform(get(TOURNAMENTS_API + "/404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath(STATUS_JSON_PATH).value(404))
        .andExpect(jsonPath("$.message", containsString("Tournament with id 404 not found")));
  }

  @Test
  void tournamentsApi_withInvalidWinner_ShouldReturnJsonError() throws Exception {
    Long firstPlayerId = createPlayer("ApiInvalidWinnerOne");
    Long secondPlayerId = createPlayer("ApiInvalidWinnerTwo");
    Long invalidWinnerId = createPlayer("ApiInvalidWinnerThree");
    TournamentResponse tournament =
        tournamentService.createTournament(
            CreateTournamentRequest.builder()
                .name("Invalid Winner Api Cup")
                .playerCount(2)
                .seedingMode(SeedingMode.MANUAL)
                .gameFormat(GameFormat.BO1)
                .winningPoints(11)
                .bracketType(BracketType.SINGLE_ELIMINATION)
                .playerIds(List.of(firstPlayerId, secondPlayerId))
                .build());
    TournamentResponse startedTournament = tournamentService.startTournament(tournament.tournamentId());
    Long tournamentMatchId = startedTournament.matches().get(0).tournamentMatchId();

    mockMvc
        .perform(
            post(TOURNAMENTS_API + "/matches/" + tournamentMatchId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"winnerId":%d}
                    """.formatted(invalidWinnerId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("Winner must be one")));
  }

  private Long createPlayer(String nickname) {
    return playerService.createPlayer(CreatePlayerRequest.builder().nickname(nickname).build()).playerId();
  }
}
