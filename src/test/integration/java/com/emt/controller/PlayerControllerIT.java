package com.emt.controller;

import static com.emt.security.SecurityRoles.ADMIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.emt.ITBase;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.PlayerResponse;
import com.emt.service.PlayerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor
public class PlayerControllerIT extends ITBase {

  private static final String ADMIN_USERNAME = "admin";
  private static final String PLAYERS_PATH = "/players";
  private static final String REGISTER_PATH = "/players/register";

  private final MockMvc mockMvc;
  private final PlayerService playerService;

  @Test
  void getPlayers_withPreCreatedPlayer_expectResponseMatch() throws Exception {
    PlayerResponse response =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("hopondeadlock").build());

    mockMvc
        .perform(get(PLAYERS_PATH))
        .andExpectAll(
            status().isOk(),
            view().name("elo-ranking"),
            model().attributeExists("players"),
            model().attributeExists("playerPage"),
            model().attributeExists("playerRequest"),
            model().attributeExists("matchRequest"),
            model().attribute("players", List.of(response)));
  }

  @Test
  void getPlayers_withSearchQuery_expectFilteredLeaderboard() throws Exception {
    PlayerResponse response =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("searchable-player").build());
    playerService.createPlayer(CreatePlayerRequest.builder().nickname("hidden-player").build());

    mockMvc
        .perform(get(PLAYERS_PATH).param("query", "searchable"))
        .andExpectAll(
            status().isOk(),
            view().name("elo-ranking"),
            model().attribute("players", List.of(response)),
            model().attribute("query", "searchable"));
  }

  @Test
  void getPlayerProfile_withExistingPlayer_expectProfilePage() throws Exception {
    PlayerResponse response =
        playerService.createPlayer(CreatePlayerRequest.builder().nickname("profile-player").build());

    mockMvc
        .perform(get(PLAYERS_PATH + "/" + response.playerId()))
        .andExpectAll(
            status().isOk(),
            view().name("player-profile"),
            model().attributeExists("profile"));
  }

  @Test
  void createPlayer_withBlankNickname_expectValidationFlashMessage() throws Exception {
    mockMvc
        .perform(
            post(REGISTER_PATH)
                .with(user(ADMIN_USERNAME).roles(ADMIN))
                .with(csrf())
                .param("nickname", ""))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(PLAYERS_PATH),
            flash().attributeExists("errors"));
  }

  @Test
  void createPlayer_withoutLogin_expectRedirectToLogin() throws Exception {
    mockMvc
        .perform(post(REGISTER_PATH).with(csrf()).param("nickname", "guest-player"))
        .andExpectAll(status().is3xxRedirection(), redirectedUrlPattern("**/login"));
  }
}
