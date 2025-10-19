package com.example.quickserve360;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class EditRestaurantActivity extends AppCompatActivity {

    private EditText nameEdit, categoryEdit, cuisineEdit, locationEdit, budgetEdit, descriptionEdit, imageEdit;
    private Button saveButton;
    private DatabaseReference databaseReference;
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_restaurant);

        getSupportActionBar().setTitle("Edit Restaurant");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        restaurantId = getIntent().getStringExtra("restaurantId");

        nameEdit = findViewById(R.id.edit_name);
        categoryEdit = findViewById(R.id.edit_category);
        cuisineEdit = findViewById(R.id.edit_cuisine);
        locationEdit = findViewById(R.id.edit_location);
        budgetEdit = findViewById(R.id.edit_budget);
        descriptionEdit = findViewById(R.id.edit_description);
        imageEdit = findViewById(R.id.edit_image);
        saveButton = findViewById(R.id.save_button);

        databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants").child(restaurantId);

        loadRestaurantData();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRestaurant();
            }
        });
    }

    private void loadRestaurantData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nameEdit.setText(snapshot.child("name").getValue(String.class));
                    categoryEdit.setText(snapshot.child("category").getValue(String.class));
                    cuisineEdit.setText(snapshot.child("cuisine").getValue(String.class));
                    locationEdit.setText(snapshot.child("location").getValue(String.class));

                    Integer budget = snapshot.child("budget").getValue(Integer.class);
                    if (budget != null) {
                        budgetEdit.setText(String.valueOf(budget));
                    }

                    descriptionEdit.setText(snapshot.child("description").getValue(String.class));
                    imageEdit.setText(snapshot.child("imagePath").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditRestaurantActivity.this,
                        "Failed to load restaurant data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRestaurant() {
        String name = nameEdit.getText().toString().trim();
        String category = categoryEdit.getText().toString().trim();
        String cuisine = cuisineEdit.getText().toString().trim();
        String location = locationEdit.getText().toString().trim();
        String budgetStr = budgetEdit.getText().toString().trim();
        String description = descriptionEdit.getText().toString().trim();
        String image = imageEdit.getText().toString().trim();

        if (name.isEmpty() || category.isEmpty() || cuisine.isEmpty() ||
                location.isEmpty() || budgetStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int budget = Integer.parseInt(budgetStr);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("category", category);
        updates.put("cuisine", cuisine);
        updates.put("location", location);
        updates.put("budget", budget);
        updates.put("description", description);
        updates.put("imagePath", image);

        databaseReference.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditRestaurantActivity.this,
                            "Restaurant updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditRestaurantActivity.this,
                            "Failed to update restaurant", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}