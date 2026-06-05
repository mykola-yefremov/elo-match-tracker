package com.emt.repository;

import com.emt.entity.TournamentMatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

  @EntityGraph(attributePaths = {"tournament", "firstPlayer", "secondPlayer", "winner"})
  Optional<TournamentMatch> findWithPlayersByTournamentMatchId(Long tournamentMatchId);
}
