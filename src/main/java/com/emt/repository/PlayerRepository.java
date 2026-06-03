package com.emt.repository;

import com.emt.entity.Player;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  boolean existsByNickname(String nickname);

  Optional<Player> findByNickname(String nickname);
}
