package com.example.quickserve360;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ManageRestaurantsActivity extends AppCompatActivity {

    private RecyclerView restaurantsRecyclerView;
    private FloatingActionButton addRestaurantFab;
    private DatabaseReference databaseReference;
    private RestaurantAdapter adapter;
    private List<Restaurant> restaurantList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_restaurants);

        getSupportActionBar().setTitle("Manage Restaurants");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        restaurantsRecyclerView = findViewById(R.id.restaurants_recycler_view);
        addRestaurantFab = findViewById(R.id.add_restaurant_fab);

        databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants");

        restaurantList = new ArrayList<>();
        adapter = new RestaurantAdapter(restaurantList);

        restaurantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        restaurantsRecyclerView.setAdapter(adapter);

        loadRestaurants();

        addRestaurantFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRestaurantDialog();
            }
        });
    }

    private void loadRestaurants() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurantList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        restaurantList.add(restaurant);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageRestaurantsActivity.this,
                        "Failed to load restaurants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddRestaurantDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_restaurant, null);
        builder.setView(dialogView);

        EditText nameEdit = dialogView.findViewById(R.id.restaurant_name_edit);
        EditText categoryEdit = dialogView.findViewById(R.id.restaurant_category_edit);
        EditText cuisineEdit = dialogView.findViewById(R.id.restaurant_cuisine_edit);
        EditText locationEdit = dialogView.findViewById(R.id.restaurant_location_edit);
        EditText budgetEdit = dialogView.findViewById(R.id.restaurant_budget_edit);
        EditText descriptionEdit = dialogView.findViewById(R.id.restaurant_description_edit);
        EditText imageEdit = dialogView.findViewById(R.id.restaurant_image_edit);
        Button addButton = dialogView.findViewById(R.id.add_restaurant_button);

        AlertDialog dialog = builder.create();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEdit.getText().toString().trim();
                String category = categoryEdit.getText().toString().trim();
                String cuisine = cuisineEdit.getText().toString().trim();
                String location = locationEdit.getText().toString().trim();
                String budgetStr = budgetEdit.getText().toString().trim();
                String description = descriptionEdit.getText().toString().trim();
                String image = imageEdit.getText().toString().trim();

                if (name.isEmpty() || category.isEmpty() || cuisine.isEmpty() ||
                        location.isEmpty() || budgetStr.isEmpty()) {
                    Toast.makeText(ManageRestaurantsActivity.this,
                            "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                int budget = Integer.parseInt(budgetStr);
                String id = "rest" + System.currentTimeMillis();

                HashMap<String, Object> restaurantMap = new HashMap<>();
                restaurantMap.put("id", id);
                restaurantMap.put("name", name);
                restaurantMap.put("category", category);
                restaurantMap.put("cuisine", cuisine);
                restaurantMap.put("location", location);
                restaurantMap.put("budget", budget);
                restaurantMap.put("description", description);
                restaurantMap.put("imagePath", image);
                restaurantMap.put("rating", 0.0);
                restaurantMap.put("isVeg", false);
                restaurantMap.put("isBestRestaurant", false);

                databaseReference.child(id).setValue(restaurantMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManageRestaurantsActivity.this,
                                    "Restaurant added successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ManageRestaurantsActivity.this,
                                    "Failed to add restaurant", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Restaurant Model Class
    public static class Restaurant {
        public String id;
        public String name;
        public String category;
        public String cuisine;
        public String location;
        public int budget;
        public String description;
        public String imagePath;
        public double rating;
        public boolean isVeg;
        public boolean isBestRestaurant;

        public Restaurant() {}
    }

    // Adapter Class
    class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

        private List<Restaurant> restaurants;

        public RestaurantAdapter(List<Restaurant> restaurants) {
            this.restaurants = restaurants;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_restaurant_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Restaurant restaurant = restaurants.get(position);

            holder.nameText.setText(restaurant.name);
            holder.categoryText.setText(restaurant.category + " • " + restaurant.cuisine);
            holder.locationText.setText(restaurant.location);
            holder.budgetText.setText("Budget: ₹" + restaurant.budget);
            holder.ratingText.setText(String.format("%.1f", restaurant.rating));

            Picasso.get().load(restaurant.imagePath).into(holder.imageView);

            holder.editButton.setOnClickListener(v -> {
                Intent intent = new Intent(ManageRestaurantsActivity.this, EditRestaurantActivity.class);
                intent.putExtra("restaurantId", restaurant.id);
                startActivity(intent);
            });

            holder.deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(ManageRestaurantsActivity.this)
                        .setTitle("Delete Restaurant")
                        .setMessage("Are you sure you want to delete " + restaurant.name + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            databaseReference.child(restaurant.id).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ManageRestaurantsActivity.this,
                                                "Restaurant deleted", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            holder.menuButton.setOnClickListener(v -> {
                Intent intent = new Intent(ManageRestaurantsActivity.this, ManageMenuActivity.class);
                intent.putExtra("restaurantId", restaurant.id);
                intent.putExtra("restaurantName", restaurant.name);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return restaurants.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView nameText, categoryText, locationText, budgetText, ratingText;
            Button editButton, deleteButton, menuButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.restaurant_image);
                nameText = itemView.findViewById(R.id.restaurant_name);
                categoryText = itemView.findViewById(R.id.restaurant_category);
                locationText = itemView.findViewById(R.id.restaurant_location);
                budgetText = itemView.findViewById(R.id.restaurant_budget);
                ratingText = itemView.findViewById(R.id.restaurant_rating);
                editButton = itemView.findViewById(R.id.edit_button);
                deleteButton = itemView.findViewById(R.id.delete_button);
                menuButton = itemView.findViewById(R.id.menu_button);
            }
        }
    }
}