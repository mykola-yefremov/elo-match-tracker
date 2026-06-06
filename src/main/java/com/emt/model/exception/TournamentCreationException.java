package com.emt.model.exception;

public class TournamentCreationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public TournamentCreationException(String message) {
    super(message);
  }
}
