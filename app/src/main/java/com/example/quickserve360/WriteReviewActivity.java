package com.example.quickserve360;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private EditText etComment;
    private MaterialButton btnSubmit, btnBack;
    private ProgressBar progressBar;
    private TextView tvAnalysisStatus;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private SentimentAnalyzer sentimentAnalyzer;

    private String restaurantId;
    private String restaurantName;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        sentimentAnalyzer = new SentimentAnalyzer(this);

        restaurantId = getIntent().getStringExtra("restaurantId");
        restaurantName = getIntent().getStringExtra("restaurantName");

        if (restaurantId == null || restaurantName == null) {
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
        String comment = etComment.getText().toString().trim();

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        tvAnalysisStatus.setVisibility(android.view.View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnBack.setEnabled(false);

        sentimentAnalyzer.analyzeSentiment(comment, result -> {
            if (result.status == 1) {
                saveReviewToFirebase(comment, result);
            } else {
                progressBar.setVisibility(android.view.View.GONE);
                tvAnalysisStatus.setVisibility(android.view.View.GONE);
                btnSubmit.setEnabled(true);
                btnBack.setEnabled(true);
                Toast.makeText(this, "Error in sentiment analysis", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveReviewToFirebase(String comment, SentimentAnalyzer.SentimentResult sentiment) {
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

        Review review = new Review(reviewId, userId, restaurantId, restaurantName, userName, comment, timestamp);
        review.setSentimentScore(sentiment.score);
        review.setSentimentLabel(sentiment.label);

        DatabaseReference reviewsRef = database.getReference("Reviews").child(reviewId);
        reviewsRef.setValue(review)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    tvAnalysisStatus.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    btnBack.setEnabled(true);

                    Toast.makeText(this, "Review submitted! Sentiment: " + sentiment.label,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    tvAnalysisStatus.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    btnBack.setEnabled(true);
                    Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                });
    }
}
