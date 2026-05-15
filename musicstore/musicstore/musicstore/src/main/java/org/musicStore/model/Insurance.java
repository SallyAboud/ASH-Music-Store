package org.musicStore.model;

public class Insurance {
    private int insuranceId;
    private double insuranceCost;
    private String coverageDetails;

    public Insurance(int insuranceId) {
        this.insuranceId = insuranceId;
    }
    public double calculateInsurance(double orderAmount) {
        if (orderAmount < 0) throw new IllegalArgumentException("Order amount must be >= 0.");
        this.insuranceCost = orderAmount * 0.10;
        return this.insuranceCost;
    }

    public int getInsuranceId() { return insuranceId; }
    public double getInsuranceCost() { return insuranceCost; }
    public String getCoverageDetails() { return coverageDetails; }
    public void setCoverageDetails(String d) { this.coverageDetails = d; }
}

