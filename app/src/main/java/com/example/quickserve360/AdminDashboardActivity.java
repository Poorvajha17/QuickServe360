package com.example.quickserve360;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        TextView welcomeText = findViewById(R.id.admin_welcome_text);
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        welcomeText.setText("Welcome Admin: " + email);
    }
}