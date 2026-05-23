package com.paytrace.models;

import com.paytrace.models.enums.RequestStatus;
import com.paytrace.models.enums.TargetType;
import java.time.LocalDateTime;

public class ConnectionRequest {
    private String requestId;
    private String userId;
    private TargetType targetType;
    private String targetId;
    private String userAddress;
    private String userJob;
    private String userPhone;
    private String extraInfo;
    private String userAccountNumber;
    private RequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private String decidedBy;

    public ConnectionRequest() {}

    public String getRequestId()              { return requestId; }
    public void   setRequestId(String v)      { this.requestId = v; }
    public String getUserId()                 { return userId; }
    public void   setUserId(String v)         { this.userId = v; }
    public TargetType getTargetType()         { return targetType; }
    public void   setTargetType(TargetType v) { this.targetType = v; }
    public String getTargetId()               { return targetId; }
    public void   setTargetId(String v)       { this.targetId = v; }
    public String getUserAddress()            { return userAddress; }
    public void   setUserAddress(String v)    { this.userAddress = v; }
    public String getUserJob()                { return userJob; }
    public void   setUserJob(String v)        { this.userJob = v; }
    public String getUserPhone()              { return userPhone; }
    public void   setUserPhone(String v)      { this.userPhone = v; }
    public String getExtraInfo()              { return extraInfo; }
    public void   setExtraInfo(String v)      { this.extraInfo = v; }
    public RequestStatus getStatus()          { return status; }
    public void   setStatus(RequestStatus v)  { this.status = v; }
    public LocalDateTime getRequestedAt()     { return requestedAt; }
    public void   setRequestedAt(LocalDateTime v){ this.requestedAt = v; }
    public LocalDateTime getDecidedAt()       { return decidedAt; }
    public void   setDecidedAt(LocalDateTime v){ this.decidedAt = v; }
    public String getDecidedBy()              { return decidedBy; }
    public void   setDecidedBy(String v)      { this.decidedBy = v; }
    public String getUserAccountNumber()         { return userAccountNumber; }
    public void   setUserAccountNumber(String v) { this.userAccountNumber = v; }
}