package com.emt.audit;

import com.emt.entity.Match;
import com.emt.entity.Player;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HibernateAuditEventListener
    implements PostInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {

  private static final String MATCH_ENTITY = "match";
  private static final String MATCH_ID = "matchId";
  private static final String PLAYER_ENTITY = "player";
  private static final String PLAYER_ID = "playerId";

  private final EntityManagerFactory entityManagerFactory;
  private final AuditRevisionWriter auditRevisionWriter;

  @PostConstruct
  public void registerListeners() {
    EventListenerRegistry registry =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);

    registry.appendListeners(EventType.POST_INSERT, this);
    registry.appendListeners(EventType.PRE_UPDATE, this);
    registry.appendListeners(EventType.PRE_DELETE, this);
  }

  @Override
  public void onPostInsert(PostInsertEvent event) {
    writeRevision(
        event.getEntity(),
        event.getId(),
        AuditOperation.INSERT,
        event.getPersister().getPropertyNames(),
        event.getState());
  }

  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    Object[] stateBeforeUpdate =
        event.getOldState() == null ? event.getState() : event.getOldState();
    writeRevision(
        event.getEntity(),
        event.getId(),
        AuditOperation.UPDATE,
        event.getPersister().getPropertyNames(),
        stateBeforeUpdate);
    return false;
  }

  @Override
  public boolean onPreDelete(PreDeleteEvent event) {
    writeRevision(
        event.getEntity(),
        event.getId(),
        AuditOperation.DELETE,
        event.getPersister().getPropertyNames(),
        event.getDeletedState());
    return false;
  }

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    return false;
  }

  private void writeRevision(
      Object entity,
      Object entityId,
      AuditOperation operation,
      String[] propertyNames,
      Object[] propertyValues) {
    AuditedEntity auditedEntity = auditedEntity(entity);
    if (auditedEntity == null) {
      return;
    }

    auditRevisionWriter.write(
        auditedEntity.entityName(),
        asLong(entityId),
        operation,
        entityState(auditedEntity, entityId, propertyNames, propertyValues));
  }

  private Map<String, Object> entityState(
      AuditedEntity auditedEntity, Object entityId, String[] propertyNames, Object[] propertyValues) {
    Map<String, Object> entityState = new LinkedHashMap<>();
    entityState.put(auditedEntity.idProperty(), asLong(entityId));

    for (int i = 0; i < propertyNames.length; i++) {
      entityState.put(propertyNames[i], normalizedValue(propertyValues[i]));
    }

    return entityState;
  }

  private Object normalizedValue(Object value) {
    if (value == null
        || value instanceof String
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Instant) {
      return value;
    }
    if (value instanceof Player player) {
      return player.getPlayerId();
    }
    if (value instanceof Match match) {
      return match.getMatchId();
    }
    return value.toString();
  }

  private Long asLong(Object entityId) {
    if (entityId instanceof Number number) {
      return number.longValue();
    }
    throw new IllegalArgumentException("Audited entity id must be numeric: " + entityId);
  }

  private AuditedEntity auditedEntity(Object entity) {
    if (entity instanceof Player) {
      return new AuditedEntity(PLAYER_ENTITY, PLAYER_ID);
    }
    if (entity instanceof Match) {
      return new AuditedEntity(MATCH_ENTITY, MATCH_ID);
    }
    return null;
  }

  private record AuditedEntity(String entityName, String idProperty) {}
}
