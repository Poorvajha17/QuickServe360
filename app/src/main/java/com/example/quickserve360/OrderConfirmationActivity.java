package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class OrderConfirmationActivity extends AppCompatActivity {

    private TextView txtOrderId, txtOrderAmount, txtDeliveryTime, txtOrderStatus;
    private Button btnBackToHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);

        initializeViews();
        setupOrderData();
        setupClickListeners();
        setupBackPressedHandler();
    }

    private void initializeViews() {
        txtOrderId = findViewById(R.id.txtOrderId);
        txtOrderAmount = findViewById(R.id.txtOrderAmount);
        txtDeliveryTime = findViewById(R.id.txtDeliveryTime);
        txtOrderStatus = findViewById(R.id.txtOrderStatus);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        // Remove track order button reference since we're removing it
    }

    private void setupOrderData() {
        Intent intent = getIntent();
        String orderId = intent.getStringExtra("ORDER_ID");
        double totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0);

        if (orderId != null) {
            // Using string resources with placeholders
            txtOrderId.setText(getString(R.string.order_id_format, orderId));
        }

        // Using string resources with placeholders
        txtOrderAmount.setText(getString(R.string.total_amount_format, (int) totalAmount));
        txtOrderStatus.setText(R.string.order_status_confirmed);
        txtDeliveryTime.setText(R.string.delivery_time_estimate);
    }

    private void setupClickListeners() {
        btnBackToHome.setOnClickListener(v -> {
            navigateToMainActivity();
        });

        // Removed track order button click listener
    }

    private void setupBackPressedHandler() {
        // Modern way to handle back press - using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent going back to payment activity
                navigateToMainActivity();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(OrderConfirmationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}