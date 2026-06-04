package com.emt.model.tournament;

public enum BracketType {
  SINGLE_ELIMINATION("Single elimination"),
  ROUND_ROBIN("Round-robin");

  private final String displayName;

  BracketType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
