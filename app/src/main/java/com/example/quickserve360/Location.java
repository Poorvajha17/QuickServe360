package com.example.quickserve360;

public class Location {
    private int Id;
    private String Loc;

    public Location() {
        // Default constructor, often used to initialize fields if no arguments are passed
    }

    @Override
    public String toString() {
        return Loc;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getLoc() {
        return Loc;
    }

    public void setLoc(String loc) {
        Loc = loc;
    }
}
