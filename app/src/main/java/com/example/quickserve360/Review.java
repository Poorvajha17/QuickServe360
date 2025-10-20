package com.example.quickserve360;

public class Review {
    public String reviewId;
    public String userId;
    public String restaurantId;
    public String restaurantName;
    public String userName;
    public String comment;
    public long timestamp;

    public float sentimentScore;
    public String sentimentLabel;

    public Review() {} // no-arg constructor for Firebase

    public Review(String reviewId, String userId, String restaurantId, String restaurantName,
                  String userName, String comment, long timestamp) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.userName = userName;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public void setSentimentScore(float score) { this.sentimentScore = score; }
    public void setSentimentLabel(String label) { this.sentimentLabel = label; }
}
