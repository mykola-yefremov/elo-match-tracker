package com.emt.service.tournament;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.entity.TournamentParticipant;
import com.emt.model.tournament.BracketType;
import com.emt.model.tournament.TournamentMatchStatus;
import com.emt.model.tournament.TournamentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoundRobinBracketStrategy implements TournamentBracketStrategy {

  private final Clock clock;
  private final TournamentMatchFactory tournamentMatchFactory;

  @Override
  public BracketType bracketType() {
    return BracketType.ROUND_ROBIN;
  }

  @Override
  public void createInitialMatches(Tournament tournament) {
    List<Player> rotation = new ArrayList<>(seededPlayers(tournament));
    int playerCount = rotation.size();

    for (int roundNumber = 1; roundNumber < playerCount; roundNumber++) {
      for (int i = 0; i < playerCount / 2; i++) {
        tournamentMatchFactory.addMatch(
            tournament,
            roundNumber,
            i + 1,
            rotation.get(i),
            rotation.get(playerCount - 1 - i));
      }
      rotateRoundRobinPlayers(rotation);
    }
  }

  @Override
  public void progressAfterResult(Tournament tournament, Integer completedRoundNumber) {
    if (hasPendingMatches(tournament.getMatches())) {
      return;
    }

    Map<Long, Integer> winsByPlayerId = winsByPlayerId(tournament);
    Player winner =
        tournament.getParticipants().stream()
            .min(roundRobinRanking(winsByPlayerId))
            .map(participant -> participant.getPlayer())
            .orElseThrow();
    completeTournament(tournament, winner);
  }

  private void rotateRoundRobinPlayers(List<Player> players) {
    Player lastPlayer = players.remove(players.size() - 1);
    players.add(1, lastPlayer);
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

  private Map<Long, Integer> winsByPlayerId(Tournament tournament) {
    Map<Long, Integer> winsByPlayerId = new LinkedHashMap<>();
    seededPlayers(tournament).forEach(player -> winsByPlayerId.put(player.getPlayerId(), 0));
    tournament
        .getMatches()
        .forEach(
            match ->
                winsByPlayerId.computeIfPresent(
                    match.getWinner().getPlayerId(), (id, wins) -> wins + 1));
    return winsByPlayerId;
  }

  private Comparator<TournamentParticipant> roundRobinRanking(Map<Long, Integer> winsByPlayerId) {
    return Comparator
        .<TournamentParticipant>comparingInt(
            participant -> -winsByPlayerId.getOrDefault(participant.getPlayer().getPlayerId(), 0))
        .thenComparingInt(participant -> participant.getSeedNumber());
  }

  private void completeTournament(Tournament tournament, Player winner) {
    tournament.setStatus(TournamentStatus.COMPLETED);
    tournament.setWinner(winner);
    tournament.setCompletedAt(Instant.now(clock));
  }
}
