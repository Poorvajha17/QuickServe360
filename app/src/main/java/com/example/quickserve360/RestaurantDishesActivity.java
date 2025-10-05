package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.ArrayList;

public class RestaurantDishesActivity extends AppCompatActivity {

    private RecyclerView dishesRecyclerView;
    private DishAdapter dishAdapter;
    private ArrayList<Dish> dishList = new ArrayList<>();
    private DatabaseReference dbRef;
    private Button btnViewCart;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_dishes);

        ivBack = findViewById(R.id.ivBack);
        dishesRecyclerView = findViewById(R.id.dishesRecyclerView);
        dishesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dishAdapter = new DishAdapter(this, dishList);
        dishesRecyclerView.setAdapter(dishAdapter);

        // Back button functionality
        ivBack.setOnClickListener(v -> finish());

        btnViewCart = findViewById(R.id.btnViewCart);
        btnViewCart.setOnClickListener(v -> {
            startActivity(new Intent(RestaurantDishesActivity.this, CartActivity.class));
        });

        dbRef = FirebaseDatabase.getInstance().getReference();
        String restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId != null) {
            loadDishes(restaurantId);
        }
    }

    private void loadDishes(String restaurantId) {
        dbRef.child("Dishes").child(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        dishList.clear();
                        for (DataSnapshot dishSnap : snapshot.getChildren()) {
                            Dish dish = dishSnap.getValue(Dish.class);
                            if (dish != null) dishList.add(dish);
                        }
                        dishAdapter.notifyDataSetChanged();

                        if (dishList.isEmpty()) {
                            Toast.makeText(RestaurantDishesActivity.this, "No dishes found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(RestaurantDishesActivity.this, "Failed to load dishes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}