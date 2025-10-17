package com.example.quickserve360;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;

public class WriteReviewActivity extends AppCompatActivity {

    private TextView tvRestaurantName;
    private RatingBar ratingBar;
    private EditText etComment;
    private MaterialButton btnSubmit;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private String restaurantId;
    private String restaurantName;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        // Get intent data
        restaurantId = getIntent().getStringExtra("restaurantId");
        restaurantName = getIntent().getStringExtra("restaurantName");

        Log.d("WriteReview", "Received restaurantId: " + restaurantId);
        Log.d("WriteReview", "Received restaurantName: " + restaurantName);

        if (restaurantId == null || restaurantName == null) {
            Log.e("WriteReview", "Error: Restaurant information is null");
            Toast.makeText(this, "Error: Restaurant information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        Log.d("WriteReview", "User ID: " + userId);

        initViews();
        setupClickListeners();

        Log.d("WriteReview", "Activity created successfully");
    }

    private void initViews() {
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);

        tvRestaurantName.setText("Review for " + restaurantName);
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create review object
        String reviewId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        // Get user name
        String userName = "User";
        if (auth.getCurrentUser() != null) {
            if (auth.getCurrentUser().getDisplayName() != null && !auth.getCurrentUser().getDisplayName().isEmpty()) {
                userName = auth.getCurrentUser().getDisplayName();
            } else if (auth.getCurrentUser().getEmail() != null) {
                userName = auth.getCurrentUser().getEmail().split("@")[0];
            }
        }

        Review review = new Review(reviewId, userId, restaurantId, restaurantName,
                userName, rating, comment, timestamp);

        Log.d("WriteReview", "Submitting review for: " + restaurantName);

        // Save to Firebase
        DatabaseReference reviewsRef = database.getReference("Reviews").child(reviewId);
        reviewsRef.setValue(review)
                .addOnSuccessListener(aVoid -> {
                    Log.d("WriteReview", "Review submitted successfully!");
                    Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("WriteReview", "Failed to submit review: " + e.getMessage());
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}