package com.emt.model.tournament;

public enum SeedingMode {
  MANUAL("Manual"),
  RANDOM("Random");

  private final String displayName;

  SeedingMode(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
