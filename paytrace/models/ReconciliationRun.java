package com.paytrace.models;

import com.paytrace.models.enums.RunStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReconciliationRun {
    private String runId;
    private LocalDateTime startedAt;
    private List<String> strategiesUsed;
    private double fuzzyThreshold;
    private int totalMatched;
    private double matchRate;
    private String initiatedBy;
    private RunStatus status;
    private BigDecimal totalValueReconciled;
    private BigDecimal toleranceAmount;
    private int toleranceDateDays;

    public ReconciliationRun() {}

    public void start()  { this.status = RunStatus.IN_PROGRESS; this.startedAt = LocalDateTime.now(); }
    public void complete(int matched, double rate) {
        this.totalMatched = matched;
        this.matchRate = rate;
        this.status = RunStatus.COMPLETED;
    }
    public void fail(String reason) { this.status = RunStatus.FAILED; }

    public String         getRunId()                     { return runId; }
    public void           setRunId(String v)             { this.runId = v; }
    public LocalDateTime  getStartedAt()                 { return startedAt; }
    public void           setStartedAt(LocalDateTime v)  { this.startedAt = v; }
    public List<String>   getStrategiesUsed()            { return strategiesUsed; }
    public void           setStrategiesUsed(List<String> v){ this.strategiesUsed = v; }
    public double         getFuzzyThreshold()            { return fuzzyThreshold; }
    public void           setFuzzyThreshold(double v)   { this.fuzzyThreshold = v; }
    public int            getTotalMatched()              { return totalMatched; }
    public void           setTotalMatched(int v)         { this.totalMatched = v; }
    public double         getMatchRate()                 { return matchRate; }
    public void           setMatchRate(double v)         { this.matchRate = v; }
    public String         getInitiatedBy()               { return initiatedBy; }
    public void           setInitiatedBy(String v)       { this.initiatedBy = v; }
    public RunStatus      getStatus()                    { return status; }
    public void           setStatus(RunStatus v)         { this.status = v; }
    public BigDecimal     getTotalValueReconciled()       { return totalValueReconciled; }
    public void           setTotalValueReconciled(BigDecimal v){ this.totalValueReconciled = v; }
    public BigDecimal     getToleranceAmount()           { return toleranceAmount; }
    public void           setToleranceAmount(BigDecimal v){ this.toleranceAmount = v; }
    public int            getToleranceDateDays()         { return toleranceDateDays; }
    public void           setToleranceDateDays(int v)    { this.toleranceDateDays = v; }
}
