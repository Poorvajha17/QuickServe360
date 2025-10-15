package com.example.quickserve360;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class PaymentActivity extends AppCompatActivity {

    private LinearLayout billItemsContainer;
    private TextView txtFinalAmount;
    private RadioGroup radioPaymentMethod;
    private Button btnConfirmPayment, btnCancel;
    private double totalAmount;
    private int cartItemsCount;

    private DatabaseReference cartRef, ordersRef;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initializeViews();
        loadCartItems();
        setupClickListeners();
    }

    private void initializeViews() {
        billItemsContainer = findViewById(R.id.billItemsContainer);
        txtFinalAmount = findViewById(R.id.txtFinalAmount);
        radioPaymentMethod = findViewById(R.id.radioPaymentMethod);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        btnCancel = findViewById(R.id.btnCancel);

        // Get data from intent
        totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0);
        cartItemsCount = getIntent().getIntExtra("CART_ITEMS_COUNT", 0);

        txtFinalAmount.setText("Total: ₹" + (int) totalAmount);
    }

    private void loadCartItems() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cartRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("cart");

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cartItems.clear();
                billItemsContainer.removeAllViews();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);
                    if (item != null) {
                        cartItems.add(item);
                        addBillItem(item);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PaymentActivity.this, "Failed to load cart items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addBillItem(CartItem item) {
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
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> finish());

        btnConfirmPayment.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        int selectedId = radioPaymentMethod.getCheckedRadioButtonId();
        String paymentMethod = getPaymentMethod(selectedId);

        // Show loading
        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("Processing...");

        if (paymentMethod.equals("Cash on Delivery")) {
            completeOrder(paymentMethod);
        } else {
            // For online payments, you can integrate with payment gateways here
            processOnlinePayment(paymentMethod);
        }
    }

    private void processOnlinePayment(String paymentMethod) {
        // For now, we'll simulate online payment success
        // In real app, integrate with Razorpay, Stripe, etc.

        new android.os.Handler().postDelayed(() -> {
            completeOrder(paymentMethod);
        }, 2000);
    }

    private void completeOrder(String paymentMethod) {
        String orderId = generateOrderId();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Order order = new Order(
                orderId,
                userId,
                cartItems,
                totalAmount,
                paymentMethod,
                "Pending",
                new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date())
        );

        // Save order to database
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        ordersRef.child(userId).child(orderId).setValue(order)
                .addOnSuccessListener(aVoid -> {
                    // Clear cart after successful order
                    clearCart();

                    // Navigate to order confirmation
                    Intent intent = new Intent(PaymentActivity.this, OrderConfirmationActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    intent.putExtra("TOTAL_AMOUNT", totalAmount);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnConfirmPayment.setEnabled(true);
                    btnConfirmPayment.setText("Confirm Payment");
                });
    }

    private String getPaymentMethod(int selectedId) {
        if (selectedId == R.id.radioUPI) return "UPI";
        if (selectedId == R.id.radioCard) return "Card";
        return "Cash on Delivery";
    }

    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis();
    }

    private void clearCart() {
        cartRef.removeValue();
    }
}