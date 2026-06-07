package com.emt.controller;

import com.emt.model.request.CreateMatchRequest;
import com.emt.model.request.CreatePlayerRequest;
import com.emt.model.response.PlayerResponse;
import com.emt.service.PlayerService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Hidden
@Controller
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

  private final PlayerService playerService;

  @GetMapping
  public String getAllPlayers(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model) {
    Page<PlayerResponse> playerPage =
        playerService.searchPlayers(query, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
    model.addAttribute("players", playerPage.getContent());
    model.addAttribute("playerPage", playerPage);
    model.addAttribute("query", query);
    model.addAttribute("playerRequest", CreatePlayerRequest.builder().build());
    model.addAttribute("matchRequest", CreateMatchRequest.builder().build());
    return "elo-ranking";
  }

  @GetMapping("/{playerId}")
  public String getPlayerProfile(@PathVariable Long playerId, Model model) {
    model.addAttribute("profile", playerService.getPlayerProfile(playerId));
    return "player-profile";
  }

  @PostMapping("/register")
  public String createPlayer(
      @Valid @ModelAttribute("playerRequest") CreatePlayerRequest playerRequest,
      RedirectAttributes redirectAttributes) {

    playerService.createPlayer(playerRequest);
    redirectAttributes.addFlashAttribute("message", "Player added successfully!");
    return "redirect:/players";
  }
}
