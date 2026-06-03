package com.emt.repository;

import com.emt.audit.AuditOperation;
import com.emt.entity.AuditRevision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRevisionRepository extends JpaRepository<AuditRevision, Long> {

  List<AuditRevision> findByEntityNameAndEntityIdOrderByCreatedAtAsc(
      String entityName, Long entityId);

  List<AuditRevision> findByEntityNameAndOperationOrderByCreatedAtAsc(
      String entityName, AuditOperation operation);
}
