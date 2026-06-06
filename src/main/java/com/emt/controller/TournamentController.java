package com.emt.controller;

import com.emt.model.request.CreateTournamentRequest;
import com.emt.model.response.PlayerResponse;
import com.emt.model.response.TournamentResponse;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.GameFormat;
import com.emt.model.tournament.SeedingMode;
import com.emt.service.PlayerService;
import com.emt.service.TournamentService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Hidden
@Controller
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {

  private final PlayerService playerService;
  private final TournamentService tournamentService;

  @GetMapping
  public String getTournaments(Model model) {
    List<PlayerResponse> players = playerService.getAllPlayers();
    List<TournamentResponse> tournaments = tournamentService.getAllTournaments();

    model.addAttribute("players", players);
    model.addAttribute("tournaments", tournaments);
    model.addAttribute("tournamentRequest", CreateTournamentRequest.builder().build());
    model.addAttribute("playerCounts", tournamentService.supportedPlayerCounts());
    model.addAttribute("seedingModes", SeedingMode.values());
    model.addAttribute("gameFormats", GameFormat.values());
    model.addAttribute("bracketTypes", BracketType.values());
    return "tournaments";
  }

  @PostMapping
  public String createTournament(
      @Valid @ModelAttribute("tournamentRequest") CreateTournamentRequest request,
      RedirectAttributes redirectAttributes) {
    tournamentService.createTournament(request);
    redirectAttributes.addFlashAttribute("message", "Tournament created successfully!");
    return "redirect:/tournaments";
  }

  @PostMapping("/{tournamentId}/start")
  public String startTournament(
      @PathVariable Long tournamentId, RedirectAttributes redirectAttributes) {
    tournamentService.startTournament(tournamentId);
    redirectAttributes.addFlashAttribute("message", "Tournament started successfully!");
    return "redirect:/tournaments";
  }

  @PostMapping("/matches/{tournamentMatchId}/report")
  public String reportTournamentMatch(
      @PathVariable Long tournamentMatchId,
      @RequestParam Long winnerId,
      RedirectAttributes redirectAttributes) {
    tournamentService.reportTournamentMatchResult(tournamentMatchId, winnerId);
    redirectAttributes.addFlashAttribute("message", "Tournament match recorded successfully!");
    return "redirect:/tournaments";
  }
}
