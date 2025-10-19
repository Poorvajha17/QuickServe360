package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import android.widget.TextView;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView manageRestaurantsCard, manageOrdersCard, viewReviewsCard;
    private CardView viewStatsCard, logoutCard;
    private TextView totalOrdersText, totalRevenueText, totalRestaurantsText;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        manageRestaurantsCard = findViewById(R.id.manage_restaurants_card);
        manageOrdersCard = findViewById(R.id.manage_orders_card);
        viewReviewsCard = findViewById(R.id.view_reviews_card);
        viewStatsCard = findViewById(R.id.view_stats_card);
        logoutCard = findViewById(R.id.logout_card);

        totalOrdersText = findViewById(R.id.total_orders_text);
        totalRevenueText = findViewById(R.id.total_revenue_text);
        totalRestaurantsText = findViewById(R.id.total_restaurants_text);

        // Load dashboard stats
        loadDashboardStats();

        // Manage Restaurants
        manageRestaurantsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AdminDashboardActivity.this, ManageRestaurantsActivity.class));
            }
        });

        // Manage Orders
        manageOrdersCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AdminDashboardActivity.this, ManageOrdersActivity.class));
            }
        });

        // View Reviews
        viewReviewsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AdminDashboardActivity.this, ViewReviewsActivity.class));
            }
        });

        // View Statistics
        viewStatsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AdminDashboardActivity.this, AdminStatisticsActivity.class));
            }
        });

        // Logout
        logoutCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(AdminDashboardActivity.this, RoleSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadDashboardStats() {
        // Count total orders
        databaseReference.child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalOrders = 0;
                double totalRevenue = 0;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot orderSnapshot : userSnapshot.getChildren()) {
                        totalOrders++;
                        Double amount = orderSnapshot.child("totalAmount").getValue(Double.class);
                        if (amount != null) {
                            totalRevenue += amount;
                        }
                    }
                }

                totalOrdersText.setText(String.valueOf(totalOrders));
                totalRevenueText.setText("₹" + String.format("%.2f", totalRevenue));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
            }
        });

        // Count total restaurants
        databaseReference.child("Restaurants").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalRestaurantsText.setText(String.valueOf(snapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                totalRestaurantsText.setText("0");
            }
        });
    }
}