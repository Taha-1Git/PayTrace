package com.paytrace.models;

import com.paytrace.models.enums.AccountType;
import java.time.LocalDateTime;

/**
 * Abstract base for all user types in PayTrace.
 * Subclasses: AdminUser, AdministratorUser, RegularUser.
 *
 * Demonstrates: abstraction (cannot be instantiated directly),
 * encapsulation (private fields + getters/setters),
 * polymorphism (subclasses override getDashboardSummary and canAccessVendor).
 */
public abstract class User {

    private String userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private AccountType accountType;
    private boolean active;
    private boolean blocked;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private int failedAttempts;
    private LocalDateTime createdAt;

    protected User(AccountType accountType) {
        this.accountType = accountType;
    }

    /** Polymorphic — each subclass returns its own headline label for the UI. */
    public abstract String getDashboardSummary();

    /** Polymorphic — each role decides differently whether it can enter a given vendor's data. */
    public abstract boolean canAccessVendor(String vendorId);

    /** Polymorphic — short role label shown in the top bar. */
    public abstract String getRoleBadge();

    public String        getUserId()                  { return userId; }
    public void          setUserId(String v)          { this.userId = v; }
    public String        getFullName()                { return fullName; }
    public void          setFullName(String v)        { this.fullName = v; }
    public String        getEmail()                   { return email; }
    public void          setEmail(String v)           { this.email = v; }
    public String        getPasswordHash()            { return passwordHash; }
    public void          setPasswordHash(String v)    { this.passwordHash = v; }
    public AccountType   getAccountType()             { return accountType; }
    public boolean       isActive()                   { return active; }
    public void          setActive(boolean v)         { this.active = v; }
    public boolean       isBlocked()                  { return blocked; }
    public void          setBlocked(boolean v)        { this.blocked = v; }
    public String        getBlockedReason()           { return blockedReason; }
    public void          setBlockedReason(String v)   { this.blockedReason = v; }
    public LocalDateTime getBlockedAt()               { return blockedAt; }
    public void          setBlockedAt(LocalDateTime v){ this.blockedAt = v; }
    public int           getFailedAttempts()          { return failedAttempts; }
    public void          setFailedAttempts(int v)     { this.failedAttempts = v; }
    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }
}