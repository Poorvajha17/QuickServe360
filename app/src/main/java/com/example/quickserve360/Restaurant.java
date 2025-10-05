package com.example.quickserve360;

public class Restaurant {
    private String id;
    private String name;
    private String description;
    private double rating;
    private String cuisine;
    private String category;
    private boolean isVeg;
    private String location;
    private String imagePath;
    private boolean isBestRestaurant;
    private double budget;

    // Empty constructor required for Firebase
    public Restaurant() {
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getRating() {
        return rating;
    }

    public String getCuisine() {
        return cuisine;
    }

    public String getCategory() {
        return category;
    }

    public boolean getIsVeg() {
        return isVeg;
    }

    public boolean isVeg() {
        return isVeg;
    }

    public String getLocation() {
        return location;
    }

    public String getImagePath() {
        return imagePath;
    }

    public boolean getIsBestRestaurant() {
        return isBestRestaurant;
    }

    public boolean isBestRestaurant() {
        return isBestRestaurant;
    }

    public double getBudget() {
        return budget;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setIsVeg(boolean isVeg) {
        this.isVeg = isVeg;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setIsBestRestaurant(boolean isBestRestaurant) {
        this.isBestRestaurant = isBestRestaurant;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }
}