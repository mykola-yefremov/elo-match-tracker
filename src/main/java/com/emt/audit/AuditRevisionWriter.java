package com.emt.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditRevisionWriter {

  private static final Pattern SAFE_SCHEMA_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
  private static final String INSERT_SQL =
      """
      INSERT INTO %s
          (entity_name, entity_id, operation, entity_state, actor, created_at)
      VALUES (?, ?, ?, ?::jsonb, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final AuditActorProvider auditActorProvider;
  private final String insertSql;

  public AuditRevisionWriter(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      AuditActorProvider auditActorProvider,
      @Value("${dbSchema}") String dbSchema) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.auditActorProvider = auditActorProvider;
    this.insertSql = INSERT_SQL.formatted(qualifiedAuditTable(dbSchema));
  }

  public void write(
      String entityName, Long entityId, AuditOperation operation, Map<String, Object> entityState) {
    jdbcTemplate.update(
        insertSql,
        entityName,
        entityId,
        operation.name(),
        toJson(entityState),
        auditActorProvider.currentActor(),
        Timestamp.from(Instant.now()));
  }

  private String qualifiedAuditTable(String dbSchema) {
    if (!SAFE_SCHEMA_NAME.matcher(dbSchema).matches()) {
      throw new IllegalArgumentException("Unsafe database schema name: " + dbSchema);
    }
    return dbSchema + ".audit_revision";
  }

  private String toJson(Map<String, Object> entityState) {
    try {
      return objectMapper.writeValueAsString(entityState);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize audit entity state", ex);
    }
  }
}
