package com.paytrace.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentAllocation {
    private String allocationId;
    private String paymentId;
    private String invoiceId;
    private BigDecimal allocatedAmount;
    private boolean isResidual;
    private String source;
    private LocalDateTime allocationDate;

    public PaymentAllocation() {}

    public String     getAllocationId()               { return allocationId; }
    public void       setAllocationId(String v)       { this.allocationId = v; }
    public String     getPaymentId()                  { return paymentId; }
    public void       setPaymentId(String v)          { this.paymentId = v; }
    public String     getInvoiceId()                  { return invoiceId; }
    public void       setInvoiceId(String v)          { this.invoiceId = v; }
    public BigDecimal getAllocatedAmount()             { return allocatedAmount; }
    public void       setAllocatedAmount(BigDecimal v){ this.allocatedAmount = v; }
    public boolean    isResidual()                    { return isResidual; }
    public void       setResidual(boolean v)          { this.isResidual = v; }
    public String     getSource()                     { return source; }
    public void       setSource(String v)             { this.source = v; }
    public LocalDateTime getAllocationDate()           { return allocationDate; }
    public void       setAllocationDate(LocalDateTime v){ this.allocationDate = v; }
}
