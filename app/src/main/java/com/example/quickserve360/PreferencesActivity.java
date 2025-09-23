package com.example.quickserve360;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.quickserve360.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class PreferencesActivity extends AppCompatActivity {

    private Spinner cuisineSpinner;
    private EditText budgetEditText;
    private SeekBar distanceSeekBar;
    private TextView distanceText;
    private RadioGroup foodTypeRadioGroup;
    private Button saveButton;

    private DatabaseReference databaseReference;
    private String userId;

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

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("UserPreferences");

        // Initialize UI components
        cuisineSpinner = findViewById(R.id.spinnerCuisine);
        budgetEditText = findViewById(R.id.editBudget);
        distanceSeekBar = findViewById(R.id.seekDistance);
        distanceText = findViewById(R.id.txtDistance);
        foodTypeRadioGroup = findViewById(R.id.radioGroupFood);
        saveButton = findViewById(R.id.btnSavePreferences);

        // Setup spinner values from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.cuisine_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cuisineSpinner.setAdapter(adapter);

        // Update distance text when SeekBar changes
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceText.setText("Distance: " + progress + " km");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Save button click
        saveButton.setOnClickListener(v -> savePreferences());
    }

    private void savePreferences() {
        String cuisine = cuisineSpinner.getSelectedItem().toString();
        String budget = budgetEditText.getText().toString();
        int distance = distanceSeekBar.getProgress();

        int selectedFoodTypeId = foodTypeRadioGroup.getCheckedRadioButtonId();
        String foodType = (selectedFoodTypeId == R.id.radioVeg) ? "Veg" : "Non-Veg";

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("cuisine", cuisine);
        preferences.put("budget", budget);
        preferences.put("distance", distance);
        preferences.put("foodType", foodType);

        databaseReference.child(userId).setValue(preferences)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Preferences Saved!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
