package com.paytrace.models;

import com.paytrace.models.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Invoice {
    private String invoiceId;
    private String invoiceNumber;
    private String ownerType;        // always "VENDOR" for now
    private String ownerId;          // vendor_id
    private String userId;           // NEW — the user this invoice is billed TO
    private String counterparty;     // free-text vendor/user display
    private String description;      // NEW — what the invoice is for
    private BigDecimal amountDue;
    private BigDecimal remainingBalance;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private String approvedBy;
    private String holdReason;
    private LocalDateTime createdAt;

    public Invoice() {}

    public void approve(String adminId) {
        this.status = InvoiceStatus.APPROVED;
        this.approvedBy = adminId;
    }
    public void hold(String reason) {
        this.status = InvoiceStatus.ON_HOLD;
        this.holdReason = reason;
    }
    public void reject(String reason) {
        this.status = InvoiceStatus.REJECTED;
        this.holdReason = reason;
    }
    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate)
                && status != InvoiceStatus.REJECTED;
    }
    public void updateRemainingBalance(BigDecimal paid) {
        this.remainingBalance = this.remainingBalance.subtract(paid);
    }

    public String         getInvoiceId()              { return invoiceId; }
    public void           setInvoiceId(String v)      { this.invoiceId = v; }
    public String         getInvoiceNumber()          { return invoiceNumber; }
    public void           setInvoiceNumber(String v)  { this.invoiceNumber = v; }
    public String         getOwnerType()              { return ownerType; }
    public void           setOwnerType(String v)      { this.ownerType = v; }
    public String         getOwnerId()                { return ownerId; }
    public void           setOwnerId(String v)        { this.ownerId = v; }
    public String         getUserId()                 { return userId; }
    public void           setUserId(String v)         { this.userId = v; }
    public String         getCounterparty()           { return counterparty; }
    public void           setCounterparty(String v)   { this.counterparty = v; }
    public String         getDescription()            { return description; }
    public void           setDescription(String v)    { this.description = v; }
    public BigDecimal     getAmountDue()              { return amountDue; }
    public void           setAmountDue(BigDecimal v)  { this.amountDue = v; }
    public BigDecimal     getRemainingBalance()       { return remainingBalance; }
    public void           setRemainingBalance(BigDecimal v){ this.remainingBalance = v; }
    public InvoiceStatus  getStatus()                 { return status; }
    public void           setStatus(InvoiceStatus v)  { this.status = v; }
    public LocalDate      getDueDate()                { return dueDate; }
    public void           setDueDate(LocalDate v)     { this.dueDate = v; }
    public String         getApprovedBy()             { return approvedBy; }
    public void           setApprovedBy(String v)     { this.approvedBy = v; }
    public String         getHoldReason()             { return holdReason; }
    public void           setHoldReason(String v)     { this.holdReason = v; }
    public LocalDateTime  getCreatedAt()              { return createdAt; }
    public void           setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    // Backward-compat getter for the old field name used by existing controllers
    public String getNormalizedVendorName() { return counterparty; }
    public void   setNormalizedVendorName(String v) { this.counterparty = v; }
    public String getVendorId() { return ownerId; }
    public void   setVendorId(String v) { this.ownerId = v; }
}