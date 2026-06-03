package com.emt.model.exception;

public class PlayerNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PlayerNotFoundException(Long playerId) {
    super("Player with id %s not found".formatted(playerId));
  }
}
