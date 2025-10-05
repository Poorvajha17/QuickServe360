package com.example.quickserve360;

public class CartItem {
    private String id;
    private String name;
    private double price;
    private String imagePath;
    private String description;
    private int quantity;

    // Default constructor for Firebase
    public CartItem() { }

    public CartItem(String id, String name, double price, String imagePath, String description, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
        this.description = description;
        this.quantity = quantity;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setDescription(String description) { this.description = description; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}