package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class RoleSelectionActivity extends AppCompatActivity {
    private CardView userCard, adminCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        userCard = findViewById(R.id.user_card);
        adminCard = findViewById(R.id.admin_card);

        userCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, Signup.class);
                intent.putExtra("USER_ROLE", "user");
                startActivity(intent);
            }
        });

        adminCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, Signup.class);
                intent.putExtra("USER_ROLE", "admin");
                startActivity(intent);
            }
        });
    }
}