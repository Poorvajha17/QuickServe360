package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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

public class CategoryRestaurantsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RestaurantGridAdapter adapter;
    private ArrayList<Restaurant> restaurantList = new ArrayList<>();
    private String categoryName;
    private String selectedLocation;
    private ImageView ivBack;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_restaurants);

        categoryName = getIntent().getStringExtra("categoryName");
        selectedLocation = getIntent().getStringExtra("selectedLocation");

        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        recyclerView = findViewById(R.id.recyclerCategoryRestaurants);

        tvTitle.setText(categoryName + " Restaurants");

        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new RestaurantGridAdapter(restaurantList, restaurant -> {
            Intent intent = new Intent(CategoryRestaurantsActivity.this, RestaurantDishesActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantName", restaurant.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadRestaurantsByCategory();
    }

    private void loadRestaurantsByCategory() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Restaurants");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurantList.clear();

                for (DataSnapshot item : snapshot.getChildren()) {
                    Restaurant restaurant = item.getValue(Restaurant.class);

                    if (restaurant != null &&
                            restaurant.getCategory().equals(categoryName) &&
                            restaurant.getLocation().equals(selectedLocation)) {
                        restaurantList.add(restaurant);
                    }
                }

                adapter.notifyDataSetChanged();

                if (restaurantList.isEmpty()) {
                    Toast.makeText(CategoryRestaurantsActivity.this,
                            "No " + categoryName + " restaurants in " + selectedLocation,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoryRestaurantsActivity.this,
                        "Failed to load restaurants",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}