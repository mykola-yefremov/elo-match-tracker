package com.emt.model.exception;

public class TournamentNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TournamentNotFoundException(Long tournamentId) {
    super("Tournament with id %d not found".formatted(tournamentId));
  }
}
