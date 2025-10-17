package com.example.quickserve360;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.UUID;

public class WriteReviewActivity extends AppCompatActivity {

    private TextView tvRestaurantName;
    private RatingBar ratingBar;
    private EditText etComment;
    private MaterialButton btnSubmit;
    private MaterialButton btnBack;
    private ProgressBar progressBar;
    private TextView tvAnalysisStatus;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private SentimentAnalyzer sentimentAnalyzer;

    private DecimalFormat decimalFormat = new DecimalFormat("0.0");

    private String restaurantId;
    private String restaurantName;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        // Initialize TensorFlow Lite Sentiment Analyzer
        sentimentAnalyzer = new SentimentAnalyzer(this);

        restaurantId = getIntent().getStringExtra("restaurantId");
        restaurantName = getIntent().getStringExtra("restaurantName");

        Log.d("WriteReview", "Restaurant: " + restaurantName + " (" + restaurantId + ")");

        if (restaurantId == null || restaurantName == null) {
            Log.e("WriteReview", "Missing restaurant data");
            Toast.makeText(this, "Error: Restaurant information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        tvAnalysisStatus = findViewById(R.id.tvAnalysisStatus);

        tvRestaurantName.setText("Review for " + restaurantName);
        progressBar.setVisibility(android.view.View.GONE);
        tvAnalysisStatus.setVisibility(android.view.View.GONE);
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> submitReview());
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void submitReview() {
        float userRating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (userRating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar and analysis status
        progressBar.setVisibility(android.view.View.VISIBLE);
        tvAnalysisStatus.setVisibility(android.view.View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnBack.setEnabled(false);

        // ============ TensorFlow Lite ML Analysis ============
        sentimentAnalyzer.analyzeSentiment(comment, result -> {
            if (result.status == 1) {
                float adjustedRating = sentimentAnalyzer.calculateAdjustedRating(userRating, result.score);

                // Format to 1 decimal place
                String formattedAdjusted = decimalFormat.format(adjustedRating);
                String formattedUser = decimalFormat.format(userRating);

                Log.d("WriteReview", "ML Results - Original: " + formattedUser +
                        " | Sentiment: " + result.label +
                        " | Score: " + result.score +
                        " | Adjusted: " + formattedAdjusted);

                saveReviewToFirebase(userRating, comment, result, adjustedRating);
            } else {
                progressBar.setVisibility(android.view.View.GONE);
                tvAnalysisStatus.setVisibility(android.view.View.GONE);
                btnSubmit.setEnabled(true);
                btnBack.setEnabled(true);
                Toast.makeText(WriteReviewActivity.this,
                        "Error in sentiment analysis", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveReviewToFirebase(float userRating, String comment,
                                      SentimentAnalyzer.SentimentResult sentiment,
                                      float adjustedRating) {
        String reviewId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        String userName = "User";
        if (auth.getCurrentUser() != null) {
            if (auth.getCurrentUser().getDisplayName() != null &&
                    !auth.getCurrentUser().getDisplayName().isEmpty()) {
                userName = auth.getCurrentUser().getDisplayName();
            } else if (auth.getCurrentUser().getEmail() != null) {
                userName = auth.getCurrentUser().getEmail().split("@")[0];
            }
        }

        Review review = new Review(reviewId, userId, restaurantId, restaurantName,
                userName, userRating, comment, timestamp);

        // Format adjusted rating to 1 decimal place
        float formattedAdjusted = Float.parseFloat(decimalFormat.format(adjustedRating));

        review.setSentimentScore(sentiment.score);
        review.setSentimentLabel(sentiment.label);
        review.setAdjustedRating(formattedAdjusted);

        DatabaseReference reviewsRef = database.getReference("Reviews").child(reviewId);
        reviewsRef.setValue(review)
                .addOnSuccessListener(aVoid -> {
                    Log.d("WriteReview", "Review saved successfully");
                    updateRestaurantRating(restaurantId, formattedAdjusted, sentiment.label);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    tvAnalysisStatus.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    btnBack.setEnabled(true);
                    Log.e("WriteReview", "Failed to save review: " + e.getMessage());
                    Toast.makeText(WriteReviewActivity.this,
                            "Failed to submit review", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRestaurantRating(String restaurantId, float adjustedRating, String sentimentLabel) {
        DatabaseReference restaurantRef = database.getReference("Restaurants").child(restaurantId);
        restaurantRef.child("rating").setValue(adjustedRating)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    tvAnalysisStatus.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    btnBack.setEnabled(true);

                    String formattedRating = decimalFormat.format(adjustedRating);
                    Toast.makeText(WriteReviewActivity.this,
                            "Review submitted! Rating: " + formattedRating + " ⭐ (" + sentimentLabel + ")",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    tvAnalysisStatus.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    btnBack.setEnabled(true);
                    Log.e("WriteReview", "Failed to update rating: " + e.getMessage());
                });
    }
}