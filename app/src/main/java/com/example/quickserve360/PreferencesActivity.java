package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
import java.util.HashMap;
import java.util.Map;

public class PreferencesActivity extends AppCompatActivity {

    private Spinner cuisineSpinner;
    private EditText budgetEditText;
    private RadioGroup foodTypeRadioGroup;
    private Button saveButton;
    private ImageView ivBack;

    private DatabaseReference databaseReference;
    private String userId;

    private ArrayList<String> cuisineList = new ArrayList<>();  // list for spinner

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferences);

        // Edge-to-Edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase references
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize UI
        ivBack = findViewById(R.id.ivBack);
        cuisineSpinner = findViewById(R.id.spinnerCuisine);
        budgetEditText = findViewById(R.id.editBudget);
        foodTypeRadioGroup = findViewById(R.id.radioGroupFood);
        saveButton = findViewById(R.id.btnSavePreferences);

        // Back button functionality
        ivBack.setOnClickListener(v -> finish());

        // Load cuisines from Firebase
        loadCuisinesFromFirebase();

        // Save button
        saveButton.setOnClickListener(v -> savePreferences());
    }

    private void loadCuisinesFromFirebase() {
        DatabaseReference cuisinesRef = databaseReference.child("Cuisines");

        cuisinesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cuisineList.clear();
                cuisineList.add("Select Cuisine"); // default option

                for (DataSnapshot cuisineSnap : snapshot.getChildren()) {
                    String cuisineName = cuisineSnap.child("name").getValue(String.class);
                    if (cuisineName != null) {
                        cuisineList.add(cuisineName);
                    }
                }

                // Set the adapter after loading data
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        PreferencesActivity.this,
                        android.R.layout.simple_spinner_item,
                        cuisineList
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                cuisineSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PreferencesActivity.this, "Failed to load cuisines", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePreferences() {
        String cuisine = cuisineSpinner.getSelectedItem().toString();
        String budget = budgetEditText.getText().toString();

        int selectedFoodTypeId = foodTypeRadioGroup.getCheckedRadioButtonId();
        String foodType = (selectedFoodTypeId == R.id.radioVeg) ? "Veg" : "Non-Veg";

        if (cuisine.equals("Select Cuisine") || budget.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("cuisine", cuisine);
        preferences.put("budget", budget);
        preferences.put("foodType", foodType);

        databaseReference.child("UserPreferences").child(userId).setValue(preferences)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Preferences Saved!", Toast.LENGTH_SHORT).show();

                    // Redirect to Restaurants list activity
                    Intent intent = new Intent(PreferencesActivity.this, PreferencesRestaurantsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}