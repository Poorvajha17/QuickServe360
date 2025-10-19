package com.example.quickserve360;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewReviewsActivity extends AppCompatActivity {

    private RecyclerView reviewsRecyclerView;
    private DatabaseReference reviewsReference;
    private ReviewAdapter adapter;
    private List<ReviewInfo> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reviews);

        getSupportActionBar().setTitle("Restaurant Reviews");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view);
        reviewsReference = FirebaseDatabase.getInstance().getReference("Reviews");

        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);

        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(adapter);

        loadReviews();
    }

    private void loadReviews() {
        reviewsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();

                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                    ReviewInfo review = new ReviewInfo();
                    review.reviewId = reviewSnapshot.child("id").getValue(String.class);
                    review.restaurantId = reviewSnapshot.child("restaurantId").getValue(String.class);
                    review.restaurantName = reviewSnapshot.child("restaurantName").getValue(String.class);
                    review.userName = reviewSnapshot.child("userName").getValue(String.class);
                    review.rating = reviewSnapshot.child("rating").getValue(Integer.class);
                    review.comment = reviewSnapshot.child("comment").getValue(String.class);
                    review.timestamp = reviewSnapshot.child("timestamp").getValue(Long.class);

                    if (review.restaurantName != null) {
                        reviewList.add(review);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewReviewsActivity.this,
                        "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Review Info Model
    public static class ReviewInfo {
        public String reviewId;
        public String restaurantId;
        public String restaurantName;
        public String userName;
        public Integer rating;
        public String comment;
        public Long timestamp;
    }

    // Adapter Class
    class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

        private List<ReviewInfo> reviews;

        public ReviewAdapter(List<ReviewInfo> reviews) {
            this.reviews = reviews;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReviewInfo review = reviews.get(position);

            holder.restaurantNameText.setText(review.restaurantName);
            holder.userNameText.setText("By: " + review.userName);
            holder.commentText.setText(review.comment);

            if (review.rating != null) {
                holder.ratingBar.setRating(review.rating.floatValue());
                holder.ratingText.setText(String.valueOf(review.rating) + "/5");
            }

            if (review.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                String date = sdf.format(new Date(review.timestamp));
                holder.timestampText.setText(date);
            }
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView restaurantNameText, userNameText, commentText, ratingText, timestampText;
            RatingBar ratingBar;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                restaurantNameText = itemView.findViewById(R.id.restaurant_name_text);
                userNameText = itemView.findViewById(R.id.user_name_text);
                commentText = itemView.findViewById(R.id.comment_text);
                ratingText = itemView.findViewById(R.id.rating_text);
                timestampText = itemView.findViewById(R.id.timestamp_text);
                ratingBar = itemView.findViewById(R.id.rating_bar);
            }
        }
    }
}