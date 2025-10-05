package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PreferencesRestaurantsActivity extends AppCompatActivity {

    private ListView restaurantsListView;
    private DatabaseReference dbRef;
    private String userId;
    private ImageView ivBack;

    private ArrayList<Restaurant> restaurantList = new ArrayList<>();
    private RestaurantAdapter restaurantAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferences_restaurants);

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivBack = findViewById(R.id.ivBack);
        restaurantsListView = findViewById(R.id.restaurantsListView);
        restaurantAdapter = new RestaurantAdapter(this, restaurantList);
        restaurantsListView.setAdapter(restaurantAdapter);

        // Back button functionality
        ivBack.setOnClickListener(v -> finish());

        // Firebase references
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Set click listener to open dishes page
        restaurantsListView.setOnItemClickListener((parent, view, position, id) -> {
            Restaurant selectedRestaurant = restaurantList.get(position);
            if (selectedRestaurant != null) {
                Intent intent = new Intent(PreferencesRestaurantsActivity.this, RestaurantDishesActivity.class);
                intent.putExtra("restaurantId", selectedRestaurant.getId()); // Pass restaurantId
                startActivity(intent);
            }
        });

        loadRestaurantsMatchingPreferences();
    }

    private void loadRestaurantsMatchingPreferences() {
        dbRef.child("users").child(userId).child("selectedCity")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot citySnapshot) {
                        final String selectedCity = citySnapshot.getValue(String.class);

                        if (selectedCity == null || selectedCity.isEmpty()) {
                            Toast.makeText(PreferencesRestaurantsActivity.this, "No city selected", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        dbRef.child("UserPreferences").child(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot prefsSnapshot) {
                                        if (!prefsSnapshot.exists()) {
                                            Toast.makeText(PreferencesRestaurantsActivity.this, "No preferences found", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        final String preferredCuisine = prefsSnapshot.child("cuisine").getValue(String.class);
                                        final String preferredFoodType = prefsSnapshot.child("foodType").getValue(String.class);
                                        String budgetStr = prefsSnapshot.child("budget").getValue(String.class);

                                        double preferredBudget = Double.MAX_VALUE;
                                        if (budgetStr != null) {
                                            try {
                                                preferredBudget = Double.parseDouble(budgetStr);
                                            } catch (NumberFormatException e) {
                                                preferredBudget = Double.MAX_VALUE;
                                            }
                                        }

                                        fetchRestaurants(selectedCity, preferredCuisine, preferredFoodType, preferredBudget);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(PreferencesRestaurantsActivity.this, "Error loading preferences", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(PreferencesRestaurantsActivity.this, "Error loading city", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchRestaurants(final String city, final String cuisine, final String foodType, final double budget) {
        dbRef.child("Restaurants").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                restaurantList.clear();

                for (DataSnapshot restSnap : snapshot.getChildren()) {
                    Restaurant r = restSnap.getValue(Restaurant.class);

                    if (r != null) {
                        boolean matchesCity = r.getLocation() != null && r.getLocation().equalsIgnoreCase(city);
                        boolean matchesCuisine = r.getCuisine() != null && r.getCuisine().equalsIgnoreCase(cuisine);
                        boolean matchesBudget = r.getBudget() <= budget;

                        boolean matchesFoodType = true;
                        if (foodType != null) {
                            if (foodType.equalsIgnoreCase("Veg")) matchesFoodType = r.isVeg();
                            else if (foodType.equalsIgnoreCase("Non-Veg")) matchesFoodType = !r.isVeg();
                        }

                        if (matchesCity && matchesCuisine && matchesBudget && matchesFoodType) {
                            restaurantList.add(r);
                        }
                    }
                }

                restaurantAdapter.notifyDataSetChanged();

                if (restaurantList.isEmpty()) {
                    Toast.makeText(PreferencesRestaurantsActivity.this, "No restaurants match your preferences", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PreferencesRestaurantsActivity.this, "Failed to load restaurants", Toast.LENGTH_SHORT).show();
            }
        });
    }
}