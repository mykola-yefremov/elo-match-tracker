package com.emt.model.exception;

public class InvalidMatchScoreException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidMatchScoreException() {
    super("Winner score must be greater than loser score when both scores are provided.");
  }

  public InvalidMatchScoreException(String message) {
    super(message);
  }
}
