package com.emt.model.exception;

public class MatchNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MatchNotFoundException(Long matchId) {
    super("Match not found with id %s".formatted(matchId));
  }
}
