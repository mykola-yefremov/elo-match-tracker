package com.emt.model.exception;

public class PlayerAlreadyExistsException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PlayerAlreadyExistsException(String nickname) {
    super("Player with nickname %s already exists.".formatted(nickname));
  }
}
