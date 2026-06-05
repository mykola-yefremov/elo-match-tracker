package com.emt.service.tournament;

import com.emt.entity.Tournament;
import com.emt.model.tournament.BracketType;

public interface TournamentBracketStrategy {

  BracketType bracketType();

  void createInitialMatches(Tournament tournament);

  void progressAfterResult(Tournament tournament, Integer completedRoundNumber);
}
