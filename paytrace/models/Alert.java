package com.paytrace.models;

import com.paytrace.models.enums.AlertStatus;
import com.paytrace.models.enums.AlertType;
import com.paytrace.models.enums.Severity;
import java.time.LocalDateTime;

public class Alert {
    private String alertId;
    private AlertType alertType;
    private Severity severity;
    private String description;
    private AlertStatus status;
    private int ageInDays;
    private String resolvedBy;
    private String entityId;
    private String entityType;
    private LocalDateTime createdAt;

    public Alert() {}

    public void markReviewed(String userId) { this.status = AlertStatus.REVIEWED; this.resolvedBy = userId; }
    public void dismiss(String userId)      { this.status = AlertStatus.AUTO_RESOLVED; this.resolvedBy = userId; }
    public void autoResolve()               { this.status = AlertStatus.AUTO_RESOLVED; }
    public boolean isCritical()             { return Severity.CRITICAL.equals(severity); }

    public String      getAlertId()               { return alertId; }
    public void        setAlertId(String v)        { this.alertId = v; }
    public AlertType   getAlertType()              { return alertType; }
    public void        setAlertType(AlertType v)   { this.alertType = v; }
    public Severity    getSeverity()               { return severity; }
    public void        setSeverity(Severity v)     { this.severity = v; }
    public String      getDescription()            { return description; }
    public void        setDescription(String v)    { this.description = v; }
    public AlertStatus getStatus()                 { return status; }
    public void        setStatus(AlertStatus v)    { this.status = v; }
    public int         getAgeInDays()              { return ageInDays; }
    public void        setAgeInDays(int v)         { this.ageInDays = v; }
    public String      getResolvedBy()             { return resolvedBy; }
    public void        setResolvedBy(String v)     { this.resolvedBy = v; }
    public String      getEntityId()               { return entityId; }
    public void        setEntityId(String v)       { this.entityId = v; }
    public String      getEntityType()             { return entityType; }
    public void        setEntityType(String v)     { this.entityType = v; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void        setCreatedAt(LocalDateTime v){ this.createdAt = v; }
}
