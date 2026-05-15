package org.musicStore.model;

public class Vendor extends User {
    private String vendorName;
    private String phoneNumber;
    private boolean approved;

    public Vendor() {
        super();
    }

    public Vendor(String username, String email, String vendorName, String phone) {
        super(username, null);
        this.vendorName = vendorName;
        this.phoneNumber = phone;
        this.approved = false;
    }

    public String getCompanyName() {
        return vendorName;
    }

    public void setCompanyName(String c) {
        this.vendorName = c;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String v) {
        this.vendorName = v;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String p) {
        this.phoneNumber = p;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    @Override
    public String toString() {
        return "Vendor{" + super.toString() + ", name='" + vendorName + "', approved=" + approved + "}";
    }
}