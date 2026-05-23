package com.paytrace.models;

import com.paytrace.models.enums.ImportStatus;
import com.paytrace.models.enums.ImportType;
import java.time.LocalDateTime;

public class ImportSession {
    private String sessionId;
    private ImportType importType;
    private String fileName;
    private String importedBy;
    private int successCount;
    private int failureCount;
    private ImportStatus status;
    private LocalDateTime importedAt;

    public ImportSession() {}

    public String getSessionId()                  { return sessionId; }
    public void   setSessionId(String v)          { this.sessionId = v; }
    public ImportType getImportType()             { return importType; }
    public void   setImportType(ImportType v)     { this.importType = v; }
    public String getFileName()                   { return fileName; }
    public void   setFileName(String v)           { this.fileName = v; }
    public String getImportedBy()                 { return importedBy; }
    public void   setImportedBy(String v)         { this.importedBy = v; }
    public int    getSuccessCount()               { return successCount; }
    public void   setSuccessCount(int v)          { this.successCount = v; }
    public int    getFailureCount()               { return failureCount; }
    public void   setFailureCount(int v)          { this.failureCount = v; }
    public ImportStatus getStatus()               { return status; }
    public void   setStatus(ImportStatus v)       { this.status = v; }
    public LocalDateTime getImportedAt()          { return importedAt; }
    public void   setImportedAt(LocalDateTime v)  { this.importedAt = v; }
}
