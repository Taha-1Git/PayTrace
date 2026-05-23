package com.paytrace.models;

import java.time.LocalDateTime;

public class AuditEntry {
    private String entryId;
    private String eventType;
    private String performedBy;
    private LocalDateTime performedAt;
    private String entityType;
    private String entityId;
    private String sessionId;
    private String roleDuringAction;

    public AuditEntry() {}
    public AuditEntry(String eventType, String performedBy, String entityType,
                      String entityId, String sessionId, String role) {
        this.eventType      = eventType;
        this.performedBy    = performedBy;
        this.performedAt    = LocalDateTime.now();
        this.entityType     = entityType;
        this.entityId       = entityId;
        this.sessionId      = sessionId;
        this.roleDuringAction = role;
    }

    public String        getEntryId()                  { return entryId; }
    public void          setEntryId(String v)          { this.entryId = v; }
    public String        getEventType()                { return eventType; }
    public void          setEventType(String v)        { this.eventType = v; }
    public String        getPerformedBy()              { return performedBy; }
    public void          setPerformedBy(String v)      { this.performedBy = v; }
    public LocalDateTime getPerformedAt()              { return performedAt; }
    public void          setPerformedAt(LocalDateTime v){ this.performedAt = v; }
    public String        getEntityType()               { return entityType; }
    public void          setEntityType(String v)       { this.entityType = v; }
    public String        getEntityId()                 { return entityId; }
    public void          setEntityId(String v)         { this.entityId = v; }
    public String        getSessionId()                { return sessionId; }
    public void          setSessionId(String v)        { this.sessionId = v; }
    public String        getRoleDuringAction()         { return roleDuringAction; }
    public void          setRoleDuringAction(String v) { this.roleDuringAction = v; }
}
