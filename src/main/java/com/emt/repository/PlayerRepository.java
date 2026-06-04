package com.emt.repository;

import com.emt.entity.Player;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  boolean existsByNickname(String nickname);

  Optional<Player> findByNickname(String nickname);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
                  SELECT p
                  FROM Player p
                  WHERE p.playerId IN :playerIds
                  ORDER BY p.playerId
                  """)
  List<Player> findPlayersForUpdate(@Param("playerIds") Collection<Long> playerIds);
}
