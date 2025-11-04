package com.example.quickserve360;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private TextView signupRedirectText, forgotPassword, roleTextView;
    private String expectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        forgotPassword = findViewById(R.id.forgotPassword);
        roleTextView = findViewById(R.id.role_text);

        // Get expected role from intent
        expectedRole = getIntent().getStringExtra("USER_ROLE");
        if (expectedRole == null) {
            expectedRole = "user"; // default
        }

        // Display role
        roleTextView.setText("Logging in as: " + expectedRole.toUpperCase());

        // Hide signup option for admin
        if (expectedRole.equalsIgnoreCase("admin")) {
            signupRedirectText.setVisibility(View.GONE);
        }

        // Login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = loginEmail.getText().toString().trim();
                String pass = loginPassword.getText().toString().trim();

                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (!pass.isEmpty()) {
                        // First authenticate with Firebase
                        auth.signInWithEmailAndPassword(email, pass)
                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        String userId = auth.getCurrentUser().getUid();

                                        // Verify user role from database
                                        databaseReference.child("UserRoles").child(userId)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.exists()) {
                                                            String actualRole = snapshot.child("role")
                                                                    .getValue(String.class);

                                                            if (actualRole != null && actualRole.equals(expectedRole)) {
                                                                // Role matches, proceed with login
                                                                Toast.makeText(Login.this,
                                                                        "Login Successful", Toast.LENGTH_SHORT).show();

                                                                Intent intent;
                                                                if (expectedRole.equals("admin")) {
                                                                    // Redirect to Admin Dashboard
                                                                    intent = new Intent(Login.this, AdminDashboardActivity.class);
                                                                } else {
                                                                    // Redirect to User Location Selection
                                                                    intent = new Intent(Login.this, LocationSelectionActivity.class);
                                                                }
                                                                startActivity(intent);
                                                                finish();
                                                            } else {
                                                                // Role mismatch
                                                                auth.signOut();
                                                                Toast.makeText(Login.this,
                                                                        "Invalid credentials for " + expectedRole + " login",
                                                                        Toast.LENGTH_LONG).show();
                                                            }
                                                        } else {
                                                            // No role found in database
                                                            auth.signOut();
                                                            Toast.makeText(Login.this,
                                                                    "User role not found. Please contact support.",
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        auth.signOut();
                                                        Toast.makeText(Login.this,
                                                                "Database error: " + error.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(Login.this,
                                                "Login Failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        loginPassword.setError("Password cannot be empty");
                    }
                } else if (email.isEmpty()) {
                    loginEmail.setError("Email cannot be empty");
                } else {
                    loginEmail.setError("Please enter valid email");
                }
            }
        });

        // Forgot Password
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = loginEmail.getText().toString().trim();

                if (email.isEmpty()) {
                    loginEmail.setError("Enter your registered email");
                } else {
                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(Login.this,
                                            "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(Login.this,
                                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                }
            }
        });

        // Redirect to Signup
        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Signup.class);
                intent.putExtra("USER_ROLE", expectedRole);
                startActivity(intent);
                finish();
            }
        });

        // Insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}