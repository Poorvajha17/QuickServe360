package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class PaymentActivity extends AppCompatActivity {

    private LinearLayout billItemsContainer;
    private TextView txtFinalAmount;
    private RadioGroup radioPaymentMethod;
    private Button btnConfirmPayment, btnCancel;
    private TextInputEditText etFullName, etPhone, etAddress, etCity, etPincode, etLandmark;
    private double totalAmount;
    private int cartItemsCount;

    private DatabaseReference cartRef, ordersRef;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        try {
            initializeViews();
            initializeFirebase();
            loadCartItems();
            setupClickListeners();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing payment page", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        try {
            billItemsContainer = findViewById(R.id.billItemsContainer);
            txtFinalAmount = findViewById(R.id.txtFinalAmount);
            radioPaymentMethod = findViewById(R.id.radioPaymentMethod);
            btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
            btnCancel = findViewById(R.id.btnCancel);

            etFullName = findViewById(R.id.etFullName);
            etPhone = findViewById(R.id.etPhone);
            etAddress = findViewById(R.id.etAddress);
            etCity = findViewById(R.id.etCity);
            etPincode = findViewById(R.id.etPincode);
            etLandmark = findViewById(R.id.etLandmark);

            totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0);
            cartItemsCount = getIntent().getIntExtra("CART_ITEMS_COUNT", 0);

            txtFinalAmount.setText("Total: ₹" + (int) totalAmount);
        } catch (Exception e) {
            Toast.makeText(this, "Error loading order details", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeFirebase() {
        try {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            cartRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("cart");
            ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        } catch (Exception e) {
            Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCartItems() {
        try {
            cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        cartItems.clear();
                        billItemsContainer.removeAllViews();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            CartItem item = ds.getValue(CartItem.class);
                            if (item != null) addBillItem(item);
                        }
                    } catch (Exception e) {
                        Toast.makeText(PaymentActivity.this, "Error displaying cart items", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(PaymentActivity.this, "Failed to load cart items", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error reading cart from Firebase", Toast.LENGTH_SHORT).show();
        }
    }

    private void addBillItem(CartItem item) {
        try {
            View billItemView = LayoutInflater.from(this).inflate(R.layout.item_bill, billItemsContainer, false);

            TextView txtItemName = billItemView.findViewById(R.id.txtBillItemName);
            TextView txtItemQty = billItemView.findViewById(R.id.txtBillItemQty);
            TextView txtItemPrice = billItemView.findViewById(R.id.txtBillItemPrice);
            TextView txtItemTotal = billItemView.findViewById(R.id.txtBillItemTotal);

            double itemTotal = item.getPrice() * item.getQuantity();

            txtItemName.setText(item.getName());
            txtItemQty.setText("Qty: " + item.getQuantity());
            txtItemPrice.setText("₹" + (int) item.getPrice());
            txtItemTotal.setText("₹" + (int) itemTotal);

            billItemsContainer.addView(billItemView);
        } catch (Exception e) {
            Toast.makeText(this, "Error adding bill item", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> finish());
        btnConfirmPayment.setOnClickListener(v -> {
            try {
                processPayment();
            } catch (Exception e) {
                Toast.makeText(this, "Payment processing failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processPayment() {
        try {
            if (!validateAddressFields()) return;

            int selectedId = radioPaymentMethod.getCheckedRadioButtonId();
            String paymentMethod = getPaymentMethod(selectedId);

            btnConfirmPayment.setEnabled(false);
            btnConfirmPayment.setText("Processing...");

            if (paymentMethod.equals("Cash on Delivery")) {
                completeOrder(paymentMethod);
            } else {
                processOnlinePayment(paymentMethod);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unexpected error during payment", Toast.LENGTH_SHORT).show();
            btnConfirmPayment.setEnabled(true);
            btnConfirmPayment.setText("Confirm Payment");
        }
    }

    private boolean validateAddressFields() {
        try {
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String pincode = etPincode.getText().toString().trim();

            if (fullName.isEmpty()) {
                etFullName.setError("Full name is required");
                etFullName.requestFocus();
                return false;
            }

            if (phone.isEmpty()) {
                etPhone.setError("Phone number is required");
                etPhone.requestFocus();
                return false;
            }

            if (phone.length() != 10) {
                etPhone.setError("Enter valid 10-digit phone number");
                etPhone.requestFocus();
                return false;
            }

            if (address.isEmpty()) {
                etAddress.setError("Address is required");
                etAddress.requestFocus();
                return false;
            }

            if (city.isEmpty()) {
                etCity.setError("City is required");
                etCity.requestFocus();
                return false;
            }

            if (pincode.isEmpty()) {
                etPincode.setError("Pincode is required");
                etPincode.requestFocus();
                return false;
            }

            if (pincode.length() != 6) {
                etPincode.setError("Enter valid 6-digit pincode");
                etPincode.requestFocus();
                return false;
            }

            return true;

        } catch (Exception e) {
            Toast.makeText(this, "Invalid input. Please check your details.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private String getDeliveryAddressString() {
        try {
            return etFullName.getText().toString().trim() + ", " +
                    etAddress.getText().toString().trim() + ", " +
                    etCity.getText().toString().trim() + " - " +
                    etPincode.getText().toString().trim() +
                    (etLandmark.getText().toString().trim().isEmpty() ? "" :
                            ", Near " + etLandmark.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "Error generating delivery address", Toast.LENGTH_SHORT).show();
            return "Unknown Address";
        }
    }

    private void processOnlinePayment(String paymentMethod) {
        try {
            new android.os.Handler().postDelayed(() -> {
                try {
                    completeOrder(paymentMethod);
                } catch (Exception e) {
                    Toast.makeText(this, "Order completion failed", Toast.LENGTH_SHORT).show();
                }
            }, 2000);
        } catch (Exception e) {
            Toast.makeText(this, "Payment simulation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeOrder(String paymentMethod) {
        try {
            String orderId = generateOrderId();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String deliveryAddress = getDeliveryAddressString();

            Order order = new Order(
                    orderId,
                    userId,
                    cartItems,
                    totalAmount,
                    paymentMethod,
                    "Pending",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()),
                    deliveryAddress
            );

            ordersRef.child(userId).child(orderId).setValue(order)
                    .addOnSuccessListener(aVoid -> {
                        clearCart();
                        Intent intent = new Intent(PaymentActivity.this, OrderConfirmationActivity.class);
                        intent.putExtra("ORDER_ID", orderId);
                        intent.putExtra("TOTAL_AMOUNT", totalAmount);
                        intent.putExtra("PAYMENT_METHOD", paymentMethod);
                        intent.putExtra("DELIVERY_ADDRESS", deliveryAddress);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnConfirmPayment.setEnabled(true);
                        btnConfirmPayment.setText("Confirm Payment");
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error creating order", Toast.LENGTH_SHORT).show();
        }
    }

    private String getPaymentMethod(int selectedId) {
        try {
            if (selectedId == R.id.radioUPI) return "UPI";
            if (selectedId == R.id.radioCard) return "Card";
            return "Cash on Delivery";
        } catch (Exception e) {
            Toast.makeText(this, "Error selecting payment method", Toast.LENGTH_SHORT).show();
            return "Cash on Delivery";
        }
    }

    private String generateOrderId() {
        try {
            return "ORD" + System.currentTimeMillis();
        } catch (Exception e) {
            Toast.makeText(this, "Error generating order ID", Toast.LENGTH_SHORT).show();
            return "ORD_ERROR";
        }
    }

    private void clearCart() {
        try {
            cartRef.removeValue()
                    .addOnSuccessListener(aVoid -> { })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Note: Cart not cleared automatically", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing cart", Toast.LENGTH_SHORT).show();
        }
    }
}