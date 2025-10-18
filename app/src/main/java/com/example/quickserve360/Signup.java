package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;

public class Signup extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private EditText signupEmail, signupPassword;
    private Button signupButton;
    private TextView loginRedirectText, roleTextView;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        signupEmail = findViewById(R.id.sign_email);
        signupPassword = findViewById(R.id.password_toggle);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        roleTextView = findViewById(R.id.role_text);

        // Get role from intent
        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) {
            userRole = "user"; // default
        }

        // Display role
        roleTextView.setText("Signing up as: " + userRole.toUpperCase());

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = signupEmail.getText().toString().trim();
                String pass = signupPassword.getText().toString().trim();

                if (user.isEmpty()) {
                    signupEmail.setError("Email cannot be empty");
                    return;
                }
                if (pass.isEmpty()) {
                    signupPassword.setError("Password cannot be empty");
                    return;
                }
                if (pass.length() < 6) {
                    signupPassword.setError("Password must be at least 6 characters");
                    return;
                }

                // Create user in Firebase Auth
                auth.createUserWithEmailAndPassword(user, pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    String userId = auth.getCurrentUser().getUid();

                                    // Store user role in database
                                    HashMap<String, Object> userMap = new HashMap<>();
                                    userMap.put("email", user);
                                    userMap.put("role", userRole);
                                    userMap.put("userId", userId);

                                    databaseReference.child("UserRoles").child(userId)
                                            .setValue(userMap)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(Signup.this,
                                                                "Sign Up Successful", Toast.LENGTH_SHORT).show();

                                                        Intent intent = new Intent(Signup.this, Login.class);
                                                        intent.putExtra("USER_ROLE", userRole);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Toast.makeText(Signup.this,
                                                                "Failed to save role", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(Signup.this,
                                            "Sign Up Failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Signup.this, Login.class);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
                finish();
            }
        });

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}