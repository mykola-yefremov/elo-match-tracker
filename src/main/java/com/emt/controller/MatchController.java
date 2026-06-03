package com.emt.controller;

import com.emt.model.request.CreateMatchRequest;
import com.emt.model.response.MatchResponse;
import com.emt.model.response.PlayerResponse;
import com.emt.service.MatchService;
import com.emt.service.PlayerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
      Model model) {
    List<MatchResponse> matches = matchService.getMatchHistory(playerId, opponentId);
    List<PlayerResponse> players = playerService.getAllPlayers();

    model.addAttribute("matches", matches);
    model.addAttribute("players", players);
    model.addAttribute("selectedPlayerId", playerId);
    model.addAttribute("selectedOpponentId", opponentId);
    return "match-history";
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
