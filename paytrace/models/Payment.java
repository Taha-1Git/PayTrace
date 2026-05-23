package com.paytrace.models;

import com.paytrace.models.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Payment {
    private String paymentId;
    private String transactionId;
    private String ownerType;          // "VENDOR"
    private String ownerId;            // vendor_id
    private String counterparty;       // free-text (e.g., "Alice Khan")
    private String normalizedParty;
    private String senderAccount;      // NEW — account number payment came from
    private BigDecimal amountPaid;
    private BigDecimal unallocatedAmount;
    private LocalDate paymentDate;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private boolean confirmed;
    private String confirmedBy;
    private LocalDateTime confirmedAt;
    private PaymentStatus paymentStatus;
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    public Payment() {}

    public String getPaymentId()               { return paymentId; }
    public void   setPaymentId(String v)       { this.paymentId = v; }
    public String getTransactionId()           { return transactionId; }
    public void   setTransactionId(String v)   { this.transactionId = v; }
    public String getOwnerType()               { return ownerType; }
    public void   setOwnerType(String v)       { this.ownerType = v; }
    public String getOwnerId()                 { return ownerId; }
    public void   setOwnerId(String v)         { this.ownerId = v; }
    public String getCounterparty()            { return counterparty; }
    public void   setCounterparty(String v)    { this.counterparty = v; }
    public String getNormalizedParty()         { return normalizedParty; }
    public void   setNormalizedParty(String v) { this.normalizedParty = v; }
    public String getSenderAccount()           { return senderAccount; }
    public void   setSenderAccount(String v)   { this.senderAccount = v; }
    public BigDecimal getAmountPaid()          { return amountPaid; }
    public void   setAmountPaid(BigDecimal v)  { this.amountPaid = v; }
    public BigDecimal getUnallocatedAmount()   { return unallocatedAmount; }
    public void   setUnallocatedAmount(BigDecimal v){ this.unallocatedAmount = v; }
    public LocalDate getPaymentDate()          { return paymentDate; }
    public void   setPaymentDate(LocalDate v)  { this.paymentDate = v; }
    public PaymentStatus getStatus()           { return status; }
    public void   setStatus(PaymentStatus v)   { this.status = v; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void   setCreatedAt(LocalDateTime v){ this.createdAt = v; }
    public void setPaymentStatus(PaymentStatus v) { this.paymentStatus = v; }
    // Backward-compat for existing controllers
    public String getVendorName()              { return counterparty; }
    public void   setVendorName(String v)      { this.counterparty = v; }
    public String getNormalizedVendorName()    { return normalizedParty; }
    public void   setNormalizedVendorName(String v){ this.normalizedParty = v; }
    public boolean isConfirmed()                { return confirmed; }
    public void   setConfirmed(boolean v)       { this.confirmed = v; }
    public String getConfirmedBy()              { return confirmedBy; }
    public void   setConfirmedBy(String v)      { this.confirmedBy = v; }
    public LocalDateTime getConfirmedAt()       { return confirmedAt; }
    public void   setConfirmedAt(LocalDateTime v) { this.confirmedAt = v; }
}