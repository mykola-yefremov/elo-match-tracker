package com.emt.service.tournament;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.TournamentMatchStatus;
import com.emt.model.tournament.TournamentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SingleEliminationBracketStrategy implements TournamentBracketStrategy {

  private static final int FIRST_ROUND = 1;
  private static final int FINALIST_COUNT = 1;

  private final Clock clock;
  private final TournamentMatchFactory tournamentMatchFactory;

  @Override
  public BracketType bracketType() {
    return BracketType.SINGLE_ELIMINATION;
  }

  @Override
  public void createInitialMatches(Tournament tournament) {
    List<Player> seededPlayers = seededPlayers(tournament);
    for (int i = 0; i < seededPlayers.size() / 2; i++) {
      tournamentMatchFactory.addMatch(
          tournament,
          FIRST_ROUND,
          i + 1,
          seededPlayers.get(i),
          seededPlayers.get(seededPlayers.size() - 1 - i));
    }
  }

  @Override
  public void progressAfterResult(Tournament tournament, Integer completedRoundNumber) {
    List<TournamentMatch> roundMatches = matchesInRound(tournament, completedRoundNumber);
    if (hasPendingMatches(roundMatches)) {
      return;
    }

    List<Player> winners = roundWinners(roundMatches);
    if (winners.size() == FINALIST_COUNT) {
      completeTournament(tournament, winners.get(0));
      return;
    }

    Integer nextRoundNumber = completedRoundNumber + 1;
    if (matchesInRound(tournament, nextRoundNumber).isEmpty()) {
      createNextRound(tournament, nextRoundNumber, winners);
    }
  }

  private List<Player> seededPlayers(Tournament tournament) {
    return tournament.getParticipants().stream()
        .sorted(Comparator.comparing(participant -> participant.getSeedNumber()))
        .map(participant -> participant.getPlayer())
        .toList();
  }

  private boolean hasPendingMatches(List<TournamentMatch> matches) {
    return matches.stream().anyMatch(match -> match.getStatus() != TournamentMatchStatus.COMPLETED);
  }

  private List<Player> roundWinners(List<TournamentMatch> matches) {
    return matches.stream()
        .sorted(Comparator.comparing(TournamentMatch::getMatchNumber))
        .map(TournamentMatch::getWinner)
        .toList();
  }

  private void createNextRound(Tournament tournament, Integer roundNumber, List<Player> winners) {
    for (int i = 0; i < winners.size(); i += 2) {
      tournamentMatchFactory.addMatch(
          tournament, roundNumber, (i / 2) + 1, winners.get(i), winners.get(i + 1));
    }
  }

  private List<TournamentMatch> matchesInRound(Tournament tournament, Integer roundNumber) {
    return tournament.getMatches().stream()
        .filter(match -> match.getRoundNumber().equals(roundNumber))
        .sorted(Comparator.comparing(TournamentMatch::getMatchNumber))
        .toList();
  }

  private void completeTournament(Tournament tournament, Player winner) {
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournament.setWinner(winner);
    tournament.setCompletedAt(Instant.now(clock));
  }
}
