package com.emt.entity;

import com.emt.audit.AuditOperation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_revision")
public class AuditRevision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long auditRevisionId;

  @Column(nullable = false)
  private String entityName;

  @Column(nullable = false)
  private Long entityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditOperation operation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> entityState;

  @Column(nullable = false)
  private String actor;

  @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
  private Instant createdAt;
}
