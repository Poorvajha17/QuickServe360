package com.example.quickserve360;

public class Category {
    private String name;
    private int id;
    private String imagePath;

    public Category() {
    }

    public Category(String name, int id, String imagePath) {
        this.name = name;
        this.id = id;
        this.imagePath = imagePath;
    }

    // Getter and Setter for Name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter and Setter for Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter and Setter for ImagePath
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
