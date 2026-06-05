package com.emt.service.tournament;

import com.emt.entity.Player;
import com.emt.entity.Tournament;
import com.emt.entity.TournamentMatch;
import com.emt.model.tournament.TournamentMatchStatus;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentMatchFactory {

  private final Clock clock;

  public void addMatch(
      Tournament tournament,
      Integer roundNumber,
      Integer matchNumber,
      Player firstPlayer,
      Player secondPlayer) {
    tournament
        .getMatches()
        .add(
            TournamentMatch.builder()
                .tournament(tournament)
                .roundNumber(roundNumber)
                .matchNumber(matchNumber)
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .status(TournamentMatchStatus.PENDING)
                .createdAt(Instant.now(clock))
                .build());
  }
}
