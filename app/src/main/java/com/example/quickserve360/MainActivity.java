package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quickserve360.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();

    private BestRestaurantsAdapter bestRestaurantsAdapter;
    private ArrayList<Restaurant> bestRestaurantsList = new ArrayList<>();

    private CategoryAdapter categoryAdapter;
    private ArrayList<Category> categoryList = new ArrayList<>();

    private String selectedLocation = "";
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get current user
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d("MainActivity", "User ID: " + userId);
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get selected location from Firebase
        loadUserLocation();
    }

    private void loadUserLocation() {
        DatabaseReference userRef = database.getReference("users").child(userId).child("selectedCity");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    selectedLocation = snapshot.getValue(String.class);
                    Log.d("MainActivity", "Selected location: '" + selectedLocation + "'");

                    if (selectedLocation != null && !selectedLocation.isEmpty()) {
                        // Update UI with location
                        binding.tvUserName.setText(selectedLocation);

                        // Setup RecyclerViews FIRST
                        initBestRestaurantsRecycler();
                        initCategoryRecycler();

                        // Then Load Data from Firebase
                        initBestRestaurants();
                        initCategories();

                        // Setup click listeners
                        setupClickListeners();
                    } else {
                        redirectToLocationSelector();
                    }
                } else {
                    Log.w("MainActivity", "No location found for user");
                    redirectToLocationSelector();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Error loading location", error.toException());
                redirectToLocationSelector();
            }
        });
    }

    private void redirectToLocationSelector() {
        Toast.makeText(this, "Please select your location", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LocationSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    private void initBestRestaurantsRecycler() {
        Log.d("MainActivity", "Initializing Best Restaurants RecyclerView");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerBestRestaurants.setLayoutManager(layoutManager);
        binding.recyclerBestRestaurants.setNestedScrollingEnabled(false);
        binding.recyclerBestRestaurants.setHasFixedSize(true);

        bestRestaurantsAdapter = new BestRestaurantsAdapter(bestRestaurantsList, restaurant -> {
            Intent intent = new Intent(MainActivity.this, RestaurantDishesActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantName", restaurant.getName());
            startActivity(intent);
        });
        binding.recyclerBestRestaurants.setAdapter(bestRestaurantsAdapter);

        Log.d("MainActivity", "RecyclerView initialized with adapter");
    }

    private void initCategoryRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerCategory.setLayoutManager(layoutManager);
        binding.recyclerCategory.setNestedScrollingEnabled(false);
        binding.recyclerCategory.setHasFixedSize(true);

        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            Intent intent = new Intent(MainActivity.this, CategoryRestaurantsActivity.class);
            intent.putExtra("categoryName", category.getName());
            intent.putExtra("selectedLocation", selectedLocation);
            startActivity(intent);
        });
        binding.recyclerCategory.setAdapter(categoryAdapter);
    }

    private void initBestRestaurants() {
        Log.d("MainActivity", "Starting to load restaurants from Firebase");
        DatabaseReference restaurantsRef = database.getReference("Restaurants");

        restaurantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("MainActivity", "Firebase onDataChange called");
                bestRestaurantsList.clear();

                if (snapshot.exists()) {
                    Log.d("MainActivity", "Total restaurants in DB: " + snapshot.getChildrenCount());

                    for (DataSnapshot item : snapshot.getChildren()) {
                        try {
                            Restaurant restaurant = item.getValue(Restaurant.class);

                            if (restaurant != null) {
                                Log.d("MainActivity", "Processing: " + restaurant.getName());
                                Log.d("MainActivity", "  - Location: '" + restaurant.getLocation() + "'");
                                Log.d("MainActivity", "  - Rating: " + restaurant.getRating());

                                // Filter by location and rating >= 4.0
                                boolean locationMatch = restaurant.getLocation().equals(selectedLocation);
                                boolean hasGoodRating = restaurant.getRating() >= 4.0;

                                Log.d("MainActivity", "  - Location Match: " + locationMatch);
                                Log.d("MainActivity", "  - Rating >= 4.0: " + hasGoodRating);

                                if (locationMatch && hasGoodRating) {
                                    bestRestaurantsList.add(restaurant);
                                    Log.d("MainActivity", "  ✓ ADDED to list! Total now: " + bestRestaurantsList.size());
                                } else {
                                    Log.d("MainActivity", "  ✗ NOT ADDED - locationMatch: " + locationMatch + ", hasGoodRating: " + hasGoodRating);
                                }
                            } else {
                                Log.w("MainActivity", "Restaurant object is null for item: " + item.getKey());
                            }
                        } catch (Exception e) {
                            Log.e("MainActivity", "Error parsing restaurant: " + item.getKey(), e);
                        }
                    }

                    Log.d("MainActivity", "Final list size: " + bestRestaurantsList.size());
                    bestRestaurantsAdapter.notifyDataSetChanged();

                    // Force RecyclerView to refresh
                    binding.recyclerBestRestaurants.post(() -> {
                        binding.recyclerBestRestaurants.requestLayout();
                        Log.d("MainActivity", "RecyclerView layout requested");
                    });

                    if (bestRestaurantsList.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "No restaurants with 4+ rating in " + selectedLocation,
                                Toast.LENGTH_LONG).show();
                        Log.w("MainActivity", "List is empty after filtering!");
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Loaded " + bestRestaurantsList.size() + " top-rated restaurants",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w("MainActivity", "No Restaurants data found in Firebase");
                    Toast.makeText(MainActivity.this,
                            "No restaurants data in database",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Error loading restaurants", error.toException());
                Toast.makeText(MainActivity.this, "Failed to load restaurants: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initCategories() {
        DatabaseReference categoryRef = database.getReference("Category");

        categoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot item : snapshot.getChildren()) {
                        Category category = item.getValue(Category.class);
                        if (category != null) {
                            categoryList.add(category);
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                    Log.d("MainActivity", "Categories loaded: " + categoryList.size());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Error loading categories", error.toException());
            }
        });
    }

    private void setupClickListeners() {
        binding.btnSearchByPreferences.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
            intent.putExtra("selectedLocation", selectedLocation);
            startActivity(intent);
        });

        binding.ivLogout.setOnClickListener(v -> {
            database.getReference("users").child(userId).child("selectedCity").removeValue();
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            finish();
        });

        binding.ivCart.setOnClickListener(v -> {
            // Redirect to CartActivity
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });

        binding.tvSeeAllRestaurants.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllRestaurantsActivity.class);
            intent.putExtra("selectedLocation", selectedLocation);
            startActivity(intent);
        });
    }
}