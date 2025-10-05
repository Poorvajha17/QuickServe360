package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
import java.util.HashSet;
import java.util.Set;

public class LocationSelectionActivity extends AppCompatActivity {

    private Spinner spinnerLocation;
    private Button btnSubmit;
    private DatabaseReference db;
    private String userId;

    private ArrayList<String> locationsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_selection);

        // Adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        spinnerLocation = findViewById(R.id.spinnerLocation);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Firebase references
        db = FirebaseDatabase.getInstance().getReference();
        DatabaseReference restaurantsRef = db.child("Restaurants");

        // Get current logged-in user's UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish(); // close activity if no user
        }

        // Add default "Select City"
        locationsList.add("Select City");

        // Fetch locations from Firebase
        restaurantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Set<String> uniqueLocations = new HashSet<>();

                    for (DataSnapshot restSnapshot : snapshot.getChildren()) {
                        String location = restSnapshot.child("location").getValue(String.class);
                        if (location != null && !location.isEmpty()) {
                            uniqueLocations.add(location);
                        }
                    }

                    locationsList.addAll(uniqueLocations);

                    // Set adapter to spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            LocationSelectionActivity.this,
                            android.R.layout.simple_spinner_item,
                            locationsList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLocation.setAdapter(adapter);
                } else {
                    Toast.makeText(LocationSelectionActivity.this, "No locations found in database.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LocationSelectionActivity.this, "Failed to load locations.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save selected city to Firebase on button click
        btnSubmit.setOnClickListener(v -> {
            String selectedCity = spinnerLocation.getSelectedItem().toString();

            if ("Select City".equals(selectedCity)) {
                Toast.makeText(LocationSelectionActivity.this, "Please select a city", Toast.LENGTH_SHORT).show();
                return;
            }

            db.child("users").child(userId).child("selectedCity").setValue(selectedCity)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(LocationSelectionActivity.this, "Location saved successfully", Toast.LENGTH_SHORT).show();

                        // Redirect to PreferencesActivity
                        Intent intent = new Intent(LocationSelectionActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // close this activity
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(LocationSelectionActivity.this, "Failed to save location", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}