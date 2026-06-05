package com.emt.model.tournament;

public enum TournamentMatchStatus {
  PENDING("Pending"),
  COMPLETED("Completed");

  private final String displayName;

  TournamentMatchStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
