package com.example.quickserve360;

public class Address {
    private String fullName;
    private String phone;
    private String streetAddress;
    private String city;
    private String pincode;
    private String landmark;

    // Default constructor required for Firebase
    public Address() {
    }

    public Address(String fullName, String phone, String streetAddress, String city, String pincode, String landmark) {
        this.fullName = fullName;
        this.phone = phone;
        this.streetAddress = streetAddress;
        this.city = city;
        this.pincode = pincode;
        this.landmark = landmark;
    }

    // Getters and setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    @Override
    public String toString() {
        return fullName + ", " + phone + "\n" +
                streetAddress + ", " + city + " - " + pincode +
                (landmark != null && !landmark.isEmpty() ? "\nLandmark: " + landmark : "");
    }
}