package com.emt.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

  private final Counter matchesCreated;
  private final Counter matchesCancelled;
  private final Counter tournamentsCreated;
  private final Counter restrictedRequests;

  public BusinessMetrics(MeterRegistry meterRegistry) {
    matchesCreated = meterRegistry.counter("emt.matches.created");
    matchesCancelled = meterRegistry.counter("emt.matches.cancelled");
    tournamentsCreated = meterRegistry.counter("emt.tournaments.created");
    restrictedRequests = meterRegistry.counter("emt.requests.restricted");
  }

  public void recordMatchCreated() {
    matchesCreated.increment();
  }

  public void recordMatchCancelled() {
    matchesCancelled.increment();
  }

  public void recordTournamentCreated() {
    tournamentsCreated.increment();
  }

  public void recordRestrictedRequest() {
    restrictedRequests.increment();
  }
}
