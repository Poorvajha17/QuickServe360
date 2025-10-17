package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.List;

public class RestaurantsListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RestaurantListAdapter adapter;
    private List<Restaurant> restaurantList = new ArrayList<>();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String selectedLocation;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurants_list);

        selectedLocation = getIntent().getStringExtra("selectedLocation");
        Log.d("RestaurantsList", "Selected location: " + selectedLocation);

        // Initialize back button
        btnBack = findViewById(R.id.btnBackArrow);
        btnBack.setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recyclerRestaurants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RestaurantListAdapter(restaurantList, restaurant -> {
            Intent intent = new Intent(RestaurantsListActivity.this, WriteReviewActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantName", restaurant.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        loadRestaurants();
    }

    private void loadRestaurants() {
        DatabaseReference restaurantsRef = database.getReference("Restaurants");

        restaurantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurantList.clear();
                if (snapshot.exists()) {
                    Log.d("RestaurantsList", "Total restaurants found: " + snapshot.getChildrenCount());

                    for (DataSnapshot item : snapshot.getChildren()) {
                        Restaurant restaurant = item.getValue(Restaurant.class);
                        if (restaurant != null) {
                            // ⭐⭐⭐⭐ SET THE ID FROM FIREBASE KEY ⭐⭐⭐⭐
                            restaurant.setId(item.getKey());

                            Log.d("RestaurantsList", "Loaded restaurant: " + restaurant.getName());
                            Log.d("RestaurantsList", "Restaurant ID: " + restaurant.getId());

                            // Filter by location if selectedLocation is provided
                            if (selectedLocation == null || selectedLocation.isEmpty() ||
                                    restaurant.getLocation().equals(selectedLocation)) {
                                restaurantList.add(restaurant);
                                Log.d("RestaurantsList", "✓ ADDED: " + restaurant.getName());
                            } else {
                                Log.d("RestaurantsList", "✗ FILTERED OUT: " + restaurant.getName() + " (Location: " + restaurant.getLocation() + ")");
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("RestaurantsList", "Final list size: " + restaurantList.size());

                    if (restaurantList.isEmpty()) {
                        Toast.makeText(RestaurantsListActivity.this,
                                "No restaurants found in " + selectedLocation, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("RestaurantsList", "No restaurants data found in Firebase");
                    Toast.makeText(RestaurantsListActivity.this,
                            "No restaurants data found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RestaurantsList", "Error loading restaurants", error.toException());
                Toast.makeText(RestaurantsListActivity.this,
                        "Failed to load restaurants", Toast.LENGTH_SHORT).show();
            }
        });
    }
}