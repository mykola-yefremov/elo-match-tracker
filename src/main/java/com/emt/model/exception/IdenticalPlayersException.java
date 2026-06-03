package com.emt.model.exception;

public class IdenticalPlayersException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public IdenticalPlayersException(String message) {
    super(message);
  }
}
