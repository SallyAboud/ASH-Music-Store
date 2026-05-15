package org.musicStore.model;

public abstract class Report {
    protected int reportId;
    protected String generatedDate;

    public Report() {
        this.generatedDate = java.time.LocalDate.now().toString();
    }


    public abstract void generateReport();

    public int getReportId() { return reportId; }
    public void setReportId(int id) { this.reportId = id; }
    public String getGeneratedDate() { return generatedDate; }
}


