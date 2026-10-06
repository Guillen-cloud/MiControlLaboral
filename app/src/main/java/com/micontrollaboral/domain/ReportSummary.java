package com.micontrollaboral.domain;

import java.util.List;

public class ReportSummary {
    public final String startDate;
    public final String endDate;
    public final int generatedAmountCents;
    public final int allocatedAmountCents;
    public final int receivedAmountCents;
    public final List<EmploymentReportRow> employmentRows;

    public ReportSummary(String startDate, String endDate, int generatedAmountCents, int allocatedAmountCents, int receivedAmountCents, List<EmploymentReportRow> employmentRows) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedAmountCents = generatedAmountCents;
        this.allocatedAmountCents = allocatedAmountCents;
        this.receivedAmountCents = receivedAmountCents;
        this.employmentRows = employmentRows;
    }

    public int getPendingAmountCents() {
        return Math.max(0, generatedAmountCents - allocatedAmountCents);
    }
}