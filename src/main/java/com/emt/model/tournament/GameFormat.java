package com.emt.model.tournament;

public enum GameFormat {
  BO1("Bo1"),
  BO3("Bo3"),
  BO5("Bo5");

  private final String displayName;

  GameFormat(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
