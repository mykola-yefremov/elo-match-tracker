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
import java.math.BigDecimal;
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
  private static final String ELO_RATING_FIELD = "eloRating";
  private static final String LOSER_FIELD = "loser";
  private static final String LOSER_ID_PARAM = "loserId";
  private static final String MATCHES_PATH = "/matches";
  private static final String MATCH_ENTITY = "match";
  private static final String MATCH_ID_FIELD = "matchId";
  private static final String MATCH_ID_PARAM = "matchId";
  private static final String MATCH_REPORT_PATH = "/matches/report";
  private static final String NICKNAME_PARAM = "nickname";
  private static final String PLAYER_ENTITY = "player";
  private static final String PLAYER_ID_FIELD = "playerId";
  private static final String PLAYER_REGISTER_PATH = "/players/register";
  private static final String PLAYERS_PATH = "/players";
  private static final String SEED_ACTOR = "seed-user";
  private static final String WINNER_FIELD = "winner";
  private static final String WINNER_ID_PARAM = "winnerId";

  private final AuditRevisionRepository auditRevisionRepository;
  private final AuditProperties auditProperties;
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
            post(PLAYER_REGISTER_PATH)
                .header(ACTOR_HEADER, "portfolio-reviewer")
                .param(NICKNAME_PARAM, "AuditedPlayer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(PLAYERS_PATH));

    Player player = playerRepository.findByNickname("AuditedPlayer").orElseThrow();
    List<AuditRevision> revisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            PLAYER_ENTITY, player.getPlayerId());

    assertThat(revisions).hasSize(1);

    AuditRevision revision = revisions.get(0);
    assertThat(revision.getOperation()).isEqualTo(AuditOperation.INSERT);
    assertThat(revision.getActor()).isEqualTo("portfolio-reviewer");
    assertThat(revision.getCreatedAt()).isNotNull();
    assertThat(stateId(revision, PLAYER_ID_FIELD)).isEqualTo(player.getPlayerId());
    assertThat(revision.getEntityState()).containsEntry(NICKNAME_PARAM, "AuditedPlayer");
  }

  @Test
  void registerPlayer_withoutActorHeader_shouldUseFallbackActor() throws Exception {
    mockMvc
        .perform(post(PLAYER_REGISTER_PATH).param(NICKNAME_PARAM, "AuditedPlayerNoHeader"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(PLAYERS_PATH));

    Player player = playerRepository.findByNickname("AuditedPlayerNoHeader").orElseThrow();

    assertThat(latestPlayerRevision(player).getActor())
        .isEqualTo(auditProperties.getFallbackActor());
  }

  @Test
  void registerPlayer_withBlankActorHeader_shouldUseFallbackActor() throws Exception {
    mockMvc
        .perform(
            post(PLAYER_REGISTER_PATH)
                .header(ACTOR_HEADER, "   ")
                .param(NICKNAME_PARAM, "AuditedPlayerBlankHeader"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(PLAYERS_PATH));

    Player player = playerRepository.findByNickname("AuditedPlayerBlankHeader").orElseThrow();

    assertThat(latestPlayerRevision(player).getActor())
        .isEqualTo(auditProperties.getFallbackActor());
  }

  @Test
  void reportMatch_withActorHeader_shouldStoreMatchAndRatingRevisions() throws Exception {
    Player winner = createPlayer("AuditWinner", SEED_ACTOR);
    Player loser = createPlayer("AuditLoser", SEED_ACTOR);
    auditRevisionRepository.deleteAll();

    mockMvc
        .perform(
            post(MATCH_REPORT_PATH)
                .header(ACTOR_HEADER, "match-reporter")
                .param(WINNER_ID_PARAM, String.valueOf(winner.getPlayerId()))
                .param(LOSER_ID_PARAM, String.valueOf(loser.getPlayerId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(PLAYERS_PATH));

    Match match = matchRepository.findAll().get(0);
    List<AuditRevision> matchRevisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            MATCH_ENTITY, match.getMatchId());
    List<AuditRevision> playerUpdates =
        auditRevisionRepository.findByEntityNameAndOperationOrderByCreatedAtAsc(
            PLAYER_ENTITY, AuditOperation.UPDATE);

    assertThat(matchRevisions).hasSize(1);
    AuditRevision matchRevision = matchRevisions.get(0);
    assertThat(matchRevision.getOperation()).isEqualTo(AuditOperation.INSERT);
    assertThat(matchRevision.getActor()).isEqualTo("match-reporter");
    assertThat(matchRevision.getCreatedAt()).isNotNull();
    assertThat(stateId(matchRevision, MATCH_ID_FIELD)).isEqualTo(match.getMatchId());
    assertThat(stateId(matchRevision, WINNER_FIELD)).isEqualTo(winner.getPlayerId());
    assertThat(stateId(matchRevision, LOSER_FIELD)).isEqualTo(loser.getPlayerId());
    assertThat(matchRevision.getEntityState()).containsKeys("createdAt", "winnerRatingChange");

    assertThat(playerUpdates)
        .hasSize(2)
        .allSatisfy(
            revision -> {
              assertThat(revision.getOperation()).isEqualTo(AuditOperation.UPDATE);
              assertThat(revision.getActor()).isEqualTo("match-reporter");
              assertThat(revision.getCreatedAt()).isNotNull();
              assertThat(revision.getEntityState()).containsKeys(PLAYER_ID_FIELD, ELO_RATING_FIELD);
            });

    AuditRevision winnerRevision = playerUpdateFor(playerUpdates, winner);
    AuditRevision loserRevision = playerUpdateFor(playerUpdates, loser);
    assertThat(winnerRevision.getEntityState()).containsEntry(NICKNAME_PARAM, winner.getNickname());
    assertThat(loserRevision.getEntityState()).containsEntry(NICKNAME_PARAM, loser.getNickname());
    assertThat(stateDecimal(winnerRevision, ELO_RATING_FIELD)).isEqualByComparingTo("1200");
    assertThat(stateDecimal(loserRevision, ELO_RATING_FIELD)).isEqualByComparingTo("1200");
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
                .param(MATCH_ID_PARAM, String.valueOf(match.getMatchId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(MATCHES_PATH));

    List<AuditRevision> matchRevisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            MATCH_ENTITY, match.getMatchId());

    assertThat(matchRevisions).hasSize(1);
    assertThat(matchRevisions.get(0).getOperation()).isEqualTo(AuditOperation.DELETE);
    assertThat(matchRevisions.get(0).getActor()).isEqualTo("admin-user");
    assertThat(stateId(matchRevisions.get(0), MATCH_ID_FIELD)).isEqualTo(match.getMatchId());
    assertThat(stateId(matchRevisions.get(0), WINNER_FIELD)).isEqualTo(winner.getPlayerId());
    assertThat(stateId(matchRevisions.get(0), LOSER_FIELD)).isEqualTo(loser.getPlayerId());
  }

  private Player createPlayer(String nickname, String actor) throws Exception {
    mockMvc
        .perform(
            post(PLAYER_REGISTER_PATH).header(ACTOR_HEADER, actor).param(NICKNAME_PARAM, nickname))
        .andExpect(status().is3xxRedirection());
    return playerRepository.findByNickname(nickname).orElseThrow();
  }

  private void createMatch(Player winner, Player loser, String actor) throws Exception {
    mockMvc
        .perform(
            post(MATCH_REPORT_PATH)
                .header(ACTOR_HEADER, actor)
                .param(WINNER_ID_PARAM, String.valueOf(winner.getPlayerId()))
                .param(LOSER_ID_PARAM, String.valueOf(loser.getPlayerId())))
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

  private BigDecimal stateDecimal(AuditRevision revision, String key) {
    return new BigDecimal(revision.getEntityState().get(key).toString());
  }

  private AuditRevision latestPlayerRevision(Player player) {
    List<AuditRevision> revisions =
        auditRevisionRepository.findByEntityNameAndEntityIdOrderByCreatedAtAsc(
            PLAYER_ENTITY, player.getPlayerId());
    assertThat(revisions).isNotEmpty();
    return revisions.get(revisions.size() - 1);
  }

  private AuditRevision playerUpdateFor(List<AuditRevision> revisions, Player player) {
    return revisions.stream()
        .filter(revision -> stateId(revision, PLAYER_ID_FIELD).equals(player.getPlayerId()))
        .findFirst()
        .orElseThrow();
  }
}
