package com.emt.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emt.ITBase;
import com.emt.entity.AuditRevision;
import com.emt.entity.Match;
import com.emt.entity.Player;
import com.emt.repository.AuditRevisionRepository;
import com.emt.repository.MatchRepository;
import com.emt.repository.PlayerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditRevisionIT extends ITBase {

  private static final String ACTOR_HEADER = "X-Actor";
  private static final String MATCH_ENTITY = "match";
  private static final String PLAYER_ENTITY = "player";
  private static final String SEED_ACTOR = "seed-user";

  private final AuditRevisionRepository auditRevisionRepository;
  private final MatchRepository matchRepository;
  private final MockMvc mockMvc;
  private final PlayerRepository playerRepository;

  @BeforeEach
  void cleanDatabase() {
    deleteTestData();
  }

  @AfterEach
  void cleanDatabaseAfterTest() {
    deleteTestData();
  }

  @Test
  void registerPlayer_withActorHeader_shouldStorePlayerInsertRevision() throws Exception {
    mockMvc
        .perform(
            post("/players/register")
                .header(ACTOR_HEADER, "portfolio-reviewer")
                .param("nickname", "AuditedPlayer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players"));

    Player player = playerRepository.findByNickname("AuditedPlayer").orElseThrow();
    List<AuditRevision> revisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            PLAYER_ENTITY, player.getPlayerId());

    assertThat(revisions).hasSize(1);

    AuditRevision revision = revisions.get(0);
    assertThat(revision.getOperation()).isEqualTo(AuditOperation.INSERT);
    assertThat(revision.getActor()).isEqualTo("portfolio-reviewer");
    assertThat(revision.getCreatedAt()).isNotNull();
    assertThat(stateId(revision, "playerId")).isEqualTo(player.getPlayerId());
    assertThat(revision.getEntityState()).containsEntry("nickname", "AuditedPlayer");
  }

  @Test
  void reportMatch_withActorHeader_shouldStoreMatchAndRatingRevisions() throws Exception {
    Player winner = createPlayer("AuditWinner", SEED_ACTOR);
    Player loser = createPlayer("AuditLoser", SEED_ACTOR);
    auditRevisionRepository.deleteAll();

    mockMvc
        .perform(
            post("/matches/report")
                .header(ACTOR_HEADER, "match-reporter")
                .param("winnerId", String.valueOf(winner.getPlayerId()))
                .param("loserId", String.valueOf(loser.getPlayerId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players"));

    Match match = matchRepository.findAll().get(0);
    List<AuditRevision> matchRevisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            MATCH_ENTITY, match.getMatchId());
    List<AuditRevision> playerUpdates =
        auditRevisionRepository.findByEntityNameAndOperationOrderByCreatedAtAsc(
            PLAYER_ENTITY, AuditOperation.UPDATE);

    assertThat(matchRevisions).hasSize(1);
    assertThat(matchRevisions.get(0).getActor()).isEqualTo("match-reporter");
    assertThat(stateId(matchRevisions.get(0), "winner")).isEqualTo(winner.getPlayerId());
    assertThat(stateId(matchRevisions.get(0), "loser")).isEqualTo(loser.getPlayerId());

    assertThat(playerUpdates)
        .hasSize(2)
        .allSatisfy(revision -> assertThat(revision.getActor()).isEqualTo("match-reporter"));
  }

  @Test
  void cancelMatch_withActorHeader_shouldStoreDeletedMatchState() throws Exception {
    Player winner = createPlayer("CancelWinner", SEED_ACTOR);
    Player loser = createPlayer("CancelLoser", SEED_ACTOR);
    createMatch(winner, loser, SEED_ACTOR);
    Match match = matchRepository.findAll().get(0);
    auditRevisionRepository.deleteAll();

    mockMvc
        .perform(
            post("/matches/cancel")
                .header(ACTOR_HEADER, "admin-user")
                .param("matchId", String.valueOf(match.getMatchId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/matches"));

    List<AuditRevision> matchRevisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            MATCH_ENTITY, match.getMatchId());

    assertThat(matchRevisions).hasSize(1);
    assertThat(matchRevisions.get(0).getOperation()).isEqualTo(AuditOperation.DELETE);
    assertThat(matchRevisions.get(0).getActor()).isEqualTo("admin-user");
    assertThat(stateId(matchRevisions.get(0), "matchId")).isEqualTo(match.getMatchId());
    assertThat(stateId(matchRevisions.get(0), "winner")).isEqualTo(winner.getPlayerId());
    assertThat(stateId(matchRevisions.get(0), "loser")).isEqualTo(loser.getPlayerId());
  }

  private Player createPlayer(String nickname, String actor) throws Exception {
    mockMvc
        .perform(post("/players/register").header(ACTOR_HEADER, actor).param("nickname", nickname))
        .andExpect(status().is3xxRedirection());
    return playerRepository.findByNickname(nickname).orElseThrow();
  }

  private void createMatch(Player winner, Player loser, String actor) throws Exception {
    mockMvc
        .perform(
            post("/matches/report")
                .header(ACTOR_HEADER, actor)
                .param("winnerId", String.valueOf(winner.getPlayerId()))
                .param("loserId", String.valueOf(loser.getPlayerId())))
        .andExpect(status().is3xxRedirection());
  }

  private void deleteTestData() {
    matchRepository.deleteAllInBatch();
    playerRepository.deleteAllInBatch();
    auditRevisionRepository.deleteAllInBatch();
  }

  private Long stateId(AuditRevision revision, String key) {
    return ((Number) revision.getEntityState().get(key)).longValue();
  }
}
