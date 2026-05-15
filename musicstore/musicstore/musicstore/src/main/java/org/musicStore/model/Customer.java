package org.musicStore.model;

public class Customer extends User {
    private int customerPoints;
    private String phoneNumber;
    private String address;
    private Cart cart;
    public Customer() { super(); }

    public Customer(int points, String phone, String addr, String username) {
        super(username, null);
        this.customerPoints = points;
        this.phoneNumber = phone;
        this.address = addr;
        this.cart = new Cart(getId());
    }


    public void addPoints(int points, String phone, String addr, String username) {
        if (points < 0) throw new IllegalArgumentException("Points cannot be negative.");
        this.customerPoints += points;
        this.phoneNumber = phone;
        this.address = addr;
    }

    public void updateContactInfo(String phone, String addr) {
        this.phoneNumber = phone;
        this.address = addr;
    }


    public int getCustomerPoints() { return customerPoints; }
    public void setCustomerPoints(int p) { this.customerPoints = p; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String p) { this.phoneNumber = p; }
    public String getAddress() { return address; }
    public void setAddress(String a) { this.address = a; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    @Override
    public String toString() {
        return "Customer{" + super.toString() + ", points=" + customerPoints + "}";
    }
}

