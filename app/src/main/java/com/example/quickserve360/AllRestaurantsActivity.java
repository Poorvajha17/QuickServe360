package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AllRestaurantsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RestaurantGridAdapter adapter;
    private ArrayList<Restaurant> restaurantList = new ArrayList<>();
    private String selectedLocation;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_restaurants);

        selectedLocation = getIntent().getStringExtra("selectedLocation");

        ivBack = findViewById(R.id.ivBack);
        recyclerView = findViewById(R.id.recyclerAllRestaurants);

        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Setup RecyclerView with Grid Layout (2 columns)
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new RestaurantGridAdapter(restaurantList, restaurant -> {
            Intent intent = new Intent(AllRestaurantsActivity.this, RestaurantDishesActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantName", restaurant.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadRestaurants();
    }

    private void loadRestaurants() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Restaurants");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurantList.clear();

                for (DataSnapshot item : snapshot.getChildren()) {
                    Restaurant restaurant = item.getValue(Restaurant.class);

                    if (restaurant != null && restaurant.getLocation().equals(selectedLocation)) {
                        restaurantList.add(restaurant);
                    }
                }

                adapter.notifyDataSetChanged();

                if (restaurantList.isEmpty()) {
                    Toast.makeText(AllRestaurantsActivity.this,
                            "No restaurants found in " + selectedLocation,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AllRestaurantsActivity.this,
                        "Failed to load restaurants",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}