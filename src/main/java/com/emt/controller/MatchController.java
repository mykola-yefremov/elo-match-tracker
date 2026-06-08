package com.emt.controller;

import com.emt.model.request.CreateMatchRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.PlayerResponse;
import com.emt.service.MatchService;
import com.emt.service.PlayerService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Hidden
@Controller
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

  private final MatchService matchService;
  private final PlayerService playerService;

  @GetMapping
  public String getAllMatches(
      @RequestParam(required = false) Long playerId,
      @RequestParam(required = false) Long opponentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model) {
    Page<MatchResponse> matchPage =
        matchService.getMatchHistory(
            playerId, opponentId, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

    model.addAttribute("matches", matchPage.getContent());
    model.addAttribute("matchPage", matchPage);
    model.addAttribute("players", playerService.getAllPlayers());
    model.addAttribute("selectedPlayerId", playerId);
    model.addAttribute("selectedOpponentId", opponentId);
    return "match-history";
  }

  @GetMapping("/{matchId}")
  public String getMatch(@PathVariable Long matchId, Model model) {
    model.addAttribute("match", matchService.getMatch(matchId));
    return "match-detail";
  }

  @PostMapping("/report")
  public String reportMatch(
      @Valid @ModelAttribute CreateMatchRequest matchRequest,
      RedirectAttributes redirectAttributes) {

    matchService.createMatch(matchRequest);
    redirectAttributes.addFlashAttribute("message", "Match reported successfully!");
    return "redirect:/players";
  }

  @PostMapping("/cancel")
  public String cancelMatch(@RequestParam Long matchId, RedirectAttributes redirectAttributes) {
    matchService.cancelMatch(matchId);
    redirectAttributes.addFlashAttribute("message", "Match cancelled successfully!");
    return "redirect:/matches";
  }
}
