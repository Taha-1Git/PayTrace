package com.paytrace.models;

import com.paytrace.models.enums.OutputFormat;
import com.paytrace.models.enums.ReportType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Report {
    private String reportId;
    private ReportType reportType;
    private String generatedBy;
    private OutputFormat outputFormat;
    private LocalDate dateRangeFrom;
    private LocalDate dateRangeTo;
    private LocalDateTime generatedAt;
    private String vendorFilter;

    public Report() {}

    public String       getReportId()                  { return reportId; }
    public void         setReportId(String v)          { this.reportId = v; }
    public ReportType   getReportType()                { return reportType; }
    public void         setReportType(ReportType v)    { this.reportType = v; }
    public String       getGeneratedBy()               { return generatedBy; }
    public void         setGeneratedBy(String v)       { this.generatedBy = v; }
    public OutputFormat getOutputFormat()              { return outputFormat; }
    public void         setOutputFormat(OutputFormat v){ this.outputFormat = v; }
    public LocalDate    getDateRangeFrom()             { return dateRangeFrom; }
    public void         setDateRangeFrom(LocalDate v)  { this.dateRangeFrom = v; }
    public LocalDate    getDateRangeTo()               { return dateRangeTo; }
    public void         setDateRangeTo(LocalDate v)    { this.dateRangeTo = v; }
    public LocalDateTime getGeneratedAt()              { return generatedAt; }
    public void         setGeneratedAt(LocalDateTime v){ this.generatedAt = v; }
    public String       getVendorFilter()              { return vendorFilter; }
    public void         setVendorFilter(String v)      { this.vendorFilter = v; }
}
