package com.paytrace.models;

import com.paytrace.models.enums.AccountType;
import com.paytrace.models.enums.TargetType;
import java.time.LocalDateTime;

public class Session {
    private String sessionId;
    private String userId;
    private AccountType role;
    private LocalDateTime loginTime;
    private LocalDateTime expiryTime;
    private boolean active;
    private String ipAddress;
    // Context: which bank/vendor this user is currently viewing (null for ADMIN)
    private TargetType contextType;
    private String     contextId;
    private String     contextName;     // populated in memory only, for UI

    public Session() {}

    public String       getSessionId()             { return sessionId; }
    public void         setSessionId(String v)     { this.sessionId = v; }
    public String       getUserId()                { return userId; }
    public void         setUserId(String v)        { this.userId = v; }
    public AccountType  getRole()                  { return role; }
    public void         setRole(AccountType v)     { this.role = v; }
    public LocalDateTime getLoginTime()            { return loginTime; }
    public void         setLoginTime(LocalDateTime v){ this.loginTime = v; }
    public LocalDateTime getExpiryTime()           { return expiryTime; }
    public void         setExpiryTime(LocalDateTime v){ this.expiryTime = v; }
    public boolean      isActive()                 { return active; }
    public void         setActive(boolean v)       { this.active = v; }
    public String       getIpAddress()             { return ipAddress; }
    public void         setIpAddress(String v)     { this.ipAddress = v; }
    public TargetType   getContextType()           { return contextType; }
    public void         setContextType(TargetType v){ this.contextType = v; }
    public String       getContextId()             { return contextId; }
    public void         setContextId(String v)     { this.contextId = v; }
    public String       getContextName()           { return contextName; }
    public void         setContextName(String v)   { this.contextName = v; }
}