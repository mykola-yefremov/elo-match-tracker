package com.emt.controller;

import static com.emt.security.SecurityRoles.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.emt.ITBase;
import com.emt.model.request.CreateMatchRequest;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.PlayerResponse;
import com.emt.service.MatchService;
import com.emt.service.PlayerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@RequiredArgsConstructor
public class MatchControllerIT extends ITBase {

  private static final String MATCH_HISTORY_PATH = "/matches";
  private static final String MATCH_HISTORY_VIEW = "match-history";
  private static final String MATCHES_ATTRIBUTE = "matches";
  private static final String MATCH_CANCEL_PATH = "/matches/cancel";
  private static final String MATCH_REPORT_PATH = "/matches/report";
  private static final String OPPONENT_ID_PARAM = "opponentId";
  private static final String PLAYER_ID_PARAM = "playerId";
  private static final String SELECTED_OPPONENT_ID_ATTRIBUTE = "selectedOpponentId";
  private static final String SELECTED_PLAYER_ID_ATTRIBUTE = "selectedPlayerId";
  private static final String ADMIN_USERNAME = "admin";

  private final MockMvc mockMvc;
  private final PlayerService playerService;
  private final MatchService matchService;

  @Test
  void getAllMatches_noMatches_expectEmptyList() throws Exception {
    mockMvc
        .perform(get(MATCH_HISTORY_PATH))
        .andExpect(status().isOk())
        .andExpect(view().name(MATCH_HISTORY_VIEW))
        .andExpect(model().attributeExists(MATCHES_ATTRIBUTE))
        .andExpect(model().attributeExists("players"))
        .andExpect(model().attribute(MATCHES_ATTRIBUTE, List.of()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getAllMatches_withPlayerFilter_expectOnlyPlayerMatches() throws Exception {
    PlayerResponse alice = createPlayer("AliceOne");
    PlayerResponse bob = createPlayer("BobTwo");
    PlayerResponse carol = createPlayer("CarolThree");
    MatchResponse aliceVsBob =
        matchService.createMatch(new CreateMatchRequest(alice.playerId(), bob.playerId()));
    MatchResponse carolVsAlice =
        matchService.createMatch(new CreateMatchRequest(carol.playerId(), alice.playerId()));
    MatchResponse bobVsCarol =
        matchService.createMatch(new CreateMatchRequest(bob.playerId(), carol.playerId()));

    MvcResult result =
        mockMvc
            .perform(get(MATCH_HISTORY_PATH).param(PLAYER_ID_PARAM, String.valueOf(alice.playerId())))
            .andExpect(status().isOk())
            .andExpect(view().name(MATCH_HISTORY_VIEW))
            .andExpect(model().attribute(SELECTED_PLAYER_ID_ATTRIBUTE, alice.playerId()))
            .andExpect(model().attribute(SELECTED_OPPONENT_ID_ATTRIBUTE, nullValue()))
            .andExpect(model().attributeExists("players"))
            .andReturn();

    List<MatchResponse> matches =
        (List<MatchResponse>) result.getModelAndView().getModel().get(MATCHES_ATTRIBUTE);
    assertThat(matches)
        .extracting(MatchResponse::matchId)
        .containsExactlyInAnyOrder(aliceVsBob.matchId(), carolVsAlice.matchId())
        .doesNotContain(bobVsCarol.matchId());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getAllMatches_withPlayerPairFilter_expectOnlyHeadToHeadMatches() throws Exception {
    PlayerResponse alice = createPlayer("PairAlice");
    PlayerResponse bob = createPlayer("PairBob");
    PlayerResponse carol = createPlayer("PairCarol");
    MatchResponse aliceVsBob =
        matchService.createMatch(new CreateMatchRequest(alice.playerId(), bob.playerId()));
    matchService.createMatch(new CreateMatchRequest(carol.playerId(), alice.playerId()));

    MvcResult result =
        mockMvc
            .perform(
                get(MATCH_HISTORY_PATH)
                    .param(PLAYER_ID_PARAM, String.valueOf(alice.playerId()))
                    .param(OPPONENT_ID_PARAM, String.valueOf(bob.playerId())))
            .andExpect(status().isOk())
            .andExpect(view().name(MATCH_HISTORY_VIEW))
            .andExpect(model().attribute(SELECTED_PLAYER_ID_ATTRIBUTE, alice.playerId()))
            .andExpect(model().attribute(SELECTED_OPPONENT_ID_ATTRIBUTE, bob.playerId()))
            .andExpect(model().attributeExists("players"))
            .andReturn();

    List<MatchResponse> matches =
        (List<MatchResponse>) result.getModelAndView().getModel().get(MATCHES_ATTRIBUTE);
    assertThat(matches).extracting(MatchResponse::matchId).containsExactly(aliceVsBob.matchId());

    MvcResult reversedResult =
        mockMvc
            .perform(
                get(MATCH_HISTORY_PATH)
                    .param(PLAYER_ID_PARAM, String.valueOf(bob.playerId()))
                    .param(OPPONENT_ID_PARAM, String.valueOf(alice.playerId())))
            .andExpect(status().isOk())
            .andExpect(view().name(MATCH_HISTORY_VIEW))
            .andReturn();

    List<MatchResponse> reversedMatches =
        (List<MatchResponse>) reversedResult.getModelAndView().getModel().get(MATCHES_ATTRIBUTE);
    assertThat(reversedMatches)
        .extracting(MatchResponse::matchId)
        .containsExactly(aliceVsBob.matchId());
  }

  @Test
  void createMatch_withValidRequest_expectMatchCreated() throws Exception {
    PlayerResponse winner =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("winner").build());
    PlayerResponse loser =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("loser").build());

    mockMvc
        .perform(
            adminPost(MATCH_REPORT_PATH)
                .param("winnerId", String.valueOf(winner.playerId()))
                .param("loserId", String.valueOf(loser.playerId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players"))
        .andExpect(flash().attribute("message", "Match reported successfully!"));
  }

  @Test
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  void createMatch_withIdenticalPlayers_expectErrorFlashMessage() throws Exception {
    PlayerResponse player =
        playerService.createPlayer(
            CreatePlayerRequest.builder().nickname("duplicatePlayer").build());

    assertNotNull(player.playerId(), "PlayerId should not be null");

    mockMvc
        .perform(
            adminPost(MATCH_REPORT_PATH)
                .param("winnerId", String.valueOf(player.playerId()))
                .param("loserId", String.valueOf(player.playerId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players"))
        .andExpect(
            flash()
                .attribute(
                    "error",
                    "Match creation failed: A match cannot be created with identical players."));
  }

  @Test
  void cancelMatch_withValidMatchId_shouldCancelMatch() throws Exception {
    PlayerResponse winner =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("winner").build());
    PlayerResponse loser =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("loser").build());

    mockMvc
        .perform(
            adminPost(MATCH_REPORT_PATH)
                .param("winnerId", String.valueOf(winner.playerId()))
                .param("loserId", String.valueOf(loser.playerId())))
        .andExpect(redirectedUrl("/players"));

    List<MatchResponse> matches = matchService.getAllMatches();
    Long matchId = matches.get(0).matchId();

    mockMvc
        .perform(adminPost(MATCH_CANCEL_PATH).param("matchId", String.valueOf(matchId)))
        .andExpect(redirectedUrl(MATCH_HISTORY_PATH))
        .andExpect(flash().attribute("message", "Match cancelled successfully!"));

    List<MatchResponse> updatedMatches = matchService.getAllMatches();
    assertThat(updatedMatches).isEmpty();
  }

  private PlayerResponse createPlayer(String nickname) {
    return playerService.createPlayer(CreatePlayerRequest.builder().nickname(nickname).build());
  }

  private MockHttpServletRequestBuilder adminPost(String path) {
    return post(path).with(user(ADMIN_USERNAME).roles(ADMIN)).with(csrf());
  }
}
