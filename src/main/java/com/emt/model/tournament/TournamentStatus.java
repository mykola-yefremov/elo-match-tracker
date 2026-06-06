package com.emt.model.tournament;

public enum TournamentStatus {
  DRAFT("Draft"),
  ACTIVE("Active"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled");

  private final String displayName;

  TournamentStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
