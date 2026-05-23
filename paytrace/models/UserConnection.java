package com.paytrace.models;

import com.paytrace.models.enums.TargetType;
import java.time.LocalDateTime;

public class UserConnection {
    private String connectionId;
    private String userId;
    private TargetType targetType;
    private String targetId;
    private String accessCode;
    private String userAccountNumber;   // NEW — user's bank account for this vendor
    private LocalDateTime grantedAt;

    public UserConnection() {}

    public String getConnectionId()                  { return connectionId; }
    public void   setConnectionId(String v)          { this.connectionId = v; }
    public String getUserId()                        { return userId; }
    public void   setUserId(String v)                { this.userId = v; }
    public TargetType getTargetType()                { return targetType; }
    public void   setTargetType(TargetType v)        { this.targetType = v; }
    public String getTargetId()                      { return targetId; }
    public void   setTargetId(String v)              { this.targetId = v; }
    public String getAccessCode()                    { return accessCode; }
    public void   setAccessCode(String v)            { this.accessCode = v; }
    public String getUserAccountNumber()             { return userAccountNumber; }
    public void   setUserAccountNumber(String v)     { this.userAccountNumber = v; }
    public LocalDateTime getGrantedAt()              { return grantedAt; }
    public void   setGrantedAt(LocalDateTime v)      { this.grantedAt = v; }
}