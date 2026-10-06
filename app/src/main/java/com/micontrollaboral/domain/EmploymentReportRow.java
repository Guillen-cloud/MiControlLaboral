package com.micontrollaboral.domain;

public class EmploymentReportRow {
    public String employmentId;
    public String employmentName;
    public int generatedAmountCents;
    public int allocatedAmountCents;

    public int getPendingAmountCents() {
        return Math.max(0, generatedAmountCents - allocatedAmountCents);
    }
}