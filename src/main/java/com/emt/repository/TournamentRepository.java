package com.emt.repository;

import com.emt.entity.Tournament;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

  @Query(
      """
                  SELECT DISTINCT t
                  FROM Tournament t
                  LEFT JOIN FETCH t.participants participant
                  LEFT JOIN FETCH participant.player
                  ORDER BY t.createdAt DESC
                  """)
  List<Tournament> findAllWithParticipants();
}
