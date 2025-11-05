package com.example.quickserve360;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RatingCalculator {
    private static final String TAG = "RatingCalculator";
    private FirebaseDatabase database;

    public RatingCalculator() {
        this.database = FirebaseDatabase.getInstance();
    }

    /**
     * Updates restaurant rating based on all reviews
     * Called after a new review is submitted
     */
    public void updateRestaurantRating(String restaurantId, UpdateCallback callback) {
        DatabaseReference reviewsRef = database.getReference("Reviews");

        // Query all reviews for this restaurant
        reviewsRef.orderByChild("restaurantId").equalTo(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.w(TAG, "No reviews found for restaurant: " + restaurantId);
                            callback.onComplete(false, "No reviews found");
                            return;
                        }

                        // Calculate average rating from sentiment scores
                        double totalRating = 0;
                        int reviewCount = 0;

                        for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                            Review review = reviewSnapshot.getValue(Review.class);
                            if (review != null) {
                                // Convert sentiment score (0-1) to rating (1-5)
                                double rating = convertSentimentToRating(review.sentimentScore);
                                totalRating += rating;
                                reviewCount++;

                                Log.d(TAG, "Review sentiment: " + review.sentimentScore +
                                        " -> rating: " + rating);
                            }
                        }

                        if (reviewCount > 0) {
                            double averageRating = totalRating / reviewCount;
                            // Round to 1 decimal place
                            averageRating = Math.round(averageRating * 10.0) / 10.0;

                            Log.d(TAG, "Calculated average rating: " + averageRating +
                                    " from " + reviewCount + " reviews");

                            // Update restaurant rating in Firebase
                            updateRestaurantInDatabase(restaurantId, averageRating, callback);
                        } else {
                            callback.onComplete(false, "No valid reviews");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch reviews: " + error.getMessage());
                        callback.onComplete(false, error.getMessage());
                    }
                });
    }

    /**
     * Converts sentiment score (0-1) to restaurant rating (1-5)
     * 0.0-0.2 -> 1 star (very negative)
     * 0.2-0.4 -> 2 stars (negative)
     * 0.4-0.6 -> 3 stars (neutral)
     * 0.6-0.8 -> 4 stars (positive)
     * 0.8-1.0 -> 5 stars (very positive)
     */
    private double convertSentimentToRating(float sentimentScore) {
        // Linear mapping: sentiment [0,1] -> rating [1,5]
        double rating = 1 + (sentimentScore * 4);
        return Math.max(1.0, Math.min(5.0, rating)); // Clamp between 1 and 5
    }

    /**
     * Updates the restaurant's rating in Firebase
     */
    private void updateRestaurantInDatabase(String restaurantId, double newRating,
                                            UpdateCallback callback) {
        DatabaseReference restaurantRef = database.getReference("Restaurants").child(restaurantId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", newRating);

        restaurantRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Restaurant rating updated to: " + newRating);
                    callback.onComplete(true, "Rating updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update restaurant rating: " + e.getMessage());
                    callback.onComplete(false, e.getMessage());
                });
    }

    /**
     * Gets current rating statistics for a restaurant
     */
    public void getRatingStats(String restaurantId, StatsCallback callback) {
        DatabaseReference reviewsRef = database.getReference("Reviews");

        reviewsRef.orderByChild("restaurantId").equalTo(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        RatingStats stats = new RatingStats();

                        for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                            Review review = reviewSnapshot.getValue(Review.class);
                            if (review != null) {
                                stats.totalReviews++;
                                double rating = convertSentimentToRating(review.sentimentScore);
                                stats.totalRating += rating;

                                // Count by sentiment
                                if (review.sentimentLabel.equals("POSITIVE")) {
                                    stats.positiveCount++;
                                } else if (review.sentimentLabel.equals("NEGATIVE")) {
                                    stats.negativeCount++;
                                } else {
                                    stats.neutralCount++;
                                }
                            }
                        }

                        if (stats.totalReviews > 0) {
                            stats.averageRating = stats.totalRating / stats.totalReviews;
                            stats.averageRating = Math.round(stats.averageRating * 10.0) / 10.0;
                        }

                        callback.onStatsRetrieved(stats);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onStatsRetrieved(new RatingStats());
                    }
                });
    }

    // Callback interfaces
    public interface UpdateCallback {
        void onComplete(boolean success, String message);
    }

    public interface StatsCallback {
        void onStatsRetrieved(RatingStats stats);
    }

    // Rating statistics class
    public static class RatingStats {
        public int totalReviews = 0;
        public double averageRating = 0.0;
        public double totalRating = 0.0;
        public int positiveCount = 0;
        public int neutralCount = 0;
        public int negativeCount = 0;
    }
}