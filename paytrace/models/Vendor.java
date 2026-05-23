package com.paytrace.models;

import java.time.LocalDateTime;

public class Vendor {
    private String vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorAddress;
    private String accessCode;          // 4-digit user-facing code
    private String accountNumber;       // 10-digit "bank account" for payments
    private String administratorId;
    private LocalDateTime createdAt;

    public Vendor() {}

    public String getVendorId()                { return vendorId; }
    public void   setVendorId(String v)        { this.vendorId = v; }
    public String getVendorName()              { return vendorName; }
    public void   setVendorName(String v)      { this.vendorName = v; }
    public String getVendorEmail()             { return vendorEmail; }
    public void   setVendorEmail(String v)     { this.vendorEmail = v; }
    public String getVendorAddress()           { return vendorAddress; }
    public void   setVendorAddress(String v)   { this.vendorAddress = v; }
    public String getAccessCode()              { return accessCode; }
    public void   setAccessCode(String v)      { this.accessCode = v; }
    public String getAccountNumber()           { return accountNumber; }
    public void   setAccountNumber(String v)   { this.accountNumber = v; }
    public String getAdministratorId()         { return administratorId; }
    public void   setAdministratorId(String v) { this.administratorId = v; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void   setCreatedAt(LocalDateTime v){ this.createdAt = v; }
}