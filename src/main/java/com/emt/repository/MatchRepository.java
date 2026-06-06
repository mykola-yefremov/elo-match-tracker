package com.emt.repository;

import com.emt.entity.Match;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

  @Query(
      """
                  SELECT m
                  FROM Match m
                  JOIN FETCH m.winner
                  JOIN FETCH m.loser
                  ORDER BY m.createdAt DESC
                  """)
  List<Match> findAllWithPlayers();

  @Query(
      """
                  SELECT m
                  FROM Match m
                  JOIN FETCH m.winner
                  JOIN FETCH m.loser
                  """)
  Page<Match> findAllWithPlayers(Pageable pageable);

  @Query(
      """
                  SELECT m
                  FROM Match m
                  JOIN FETCH m.winner
                  JOIN FETCH m.loser
                  WHERE m.winner.playerId = :playerId
                     OR m.loser.playerId = :playerId
                  ORDER BY m.createdAt DESC
                  """)
  List<Match> findMatchesByPlayer(@Param("playerId") Long playerId);

  @Query(
      """
                  SELECT m
                  FROM Match m
                  JOIN FETCH m.winner
                  JOIN FETCH m.loser
                  WHERE m.winner.playerId = :playerId
                     OR m.loser.playerId = :playerId
                  """)
  Page<Match> findMatchesByPlayer(@Param("playerId") Long playerId, Pageable pageable);

  @Query(
      """
                  SELECT m
                  FROM Match m
                  JOIN FETCH m.winner
                  JOIN FETCH m.loser
                  WHERE (m.winner.playerId = :firstPlayerId AND m.loser.playerId = :secondPlayerId)
                     OR (m.winner.playerId = :secondPlayerId AND m.loser.playerId = :firstPlayerId)
                  ORDER BY m.createdAt DESC
                  """)
  List<Match> findMatchesBetweenPlayers(
      @Param("firstPlayerId") Long firstPlayerId, @Param("secondPlayerId") Long secondPlayerId);

  @Query(
      """
                  SELECT m
                  FROM Match m
                  JOIN FETCH m.winner
                  JOIN FETCH m.loser
                  WHERE (m.winner.playerId = :firstPlayerId AND m.loser.playerId = :secondPlayerId)
                     OR (m.winner.playerId = :secondPlayerId AND m.loser.playerId = :firstPlayerId)
                  """)
  Page<Match> findMatchesBetweenPlayers(
      @Param("firstPlayerId") Long firstPlayerId,
      @Param("secondPlayerId") Long secondPlayerId,
      Pageable pageable);

  @Query(
      """
                  SELECT m
                  FROM Match m
                  WHERE m.createdAt > :createdAt
                    AND (m.winner.playerId IN (:winnerId, :loserId)
                         OR m.loser.playerId IN (:winnerId, :loserId))
                  ORDER BY m.createdAt
                  """)
  List<Match> findMatchesByPlayersAfter(
      @Param("createdAt") Instant createdAt,
      @Param("winnerId") Long winnerId,
      @Param("loserId") Long loserId);
}
