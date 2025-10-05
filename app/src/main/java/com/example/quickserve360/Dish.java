package com.example.quickserve360;

public class Dish {
    private String id;
    private String name;
    private double price;
    private String imagePath;
    private String description;

    // Default constructor required for Firebase
    public Dish() {}

    public Dish(String id, String name, double price, String imagePath, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public String getDescription() { return description; }
}