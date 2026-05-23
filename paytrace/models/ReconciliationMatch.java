package com.paytrace.models;

import com.paytrace.models.enums.MatchType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReconciliationMatch {
    private String matchId;
    private String runId;
    private String invoiceId;
    private String paymentId;
    private MatchType matchType;
    private double confidenceScore;
    private String explanation;
    private String source;
    private boolean isReversed;
    private LocalDateTime matchedAt;
    private BigDecimal amountDifference;

    public ReconciliationMatch() {}

    public void reverse()    { this.isReversed = true; }
    public boolean isManual(){ return "Manual".equals(source); }

    public String       getMatchId()                    { return matchId; }
    public void         setMatchId(String v)            { this.matchId = v; }
    public String       getRunId()                      { return runId; }
    public void         setRunId(String v)              { this.runId = v; }
    public String       getInvoiceId()                  { return invoiceId; }
    public void         setInvoiceId(String v)          { this.invoiceId = v; }
    public String       getPaymentId()                  { return paymentId; }
    public void         setPaymentId(String v)          { this.paymentId = v; }
    public MatchType    getMatchType()                  { return matchType; }
    public void         setMatchType(MatchType v)       { this.matchType = v; }
    public double       getConfidenceScore()            { return confidenceScore; }
    public void         setConfidenceScore(double v)    { this.confidenceScore = v; }
    public String       getExplanation()                { return explanation; }
    public void         setExplanation(String v)        { this.explanation = v; }
    public String       getSource()                     { return source; }
    public void         setSource(String v)             { this.source = v; }
    public boolean      isReversed()                    { return isReversed; }
    public void         setReversed(boolean v)          { this.isReversed = v; }
    public LocalDateTime getMatchedAt()                 { return matchedAt; }
    public void         setMatchedAt(LocalDateTime v)   { this.matchedAt = v; }
    public BigDecimal   getAmountDifference()           { return amountDifference; }
    public void         setAmountDifference(BigDecimal v){ this.amountDifference = v; }
}
