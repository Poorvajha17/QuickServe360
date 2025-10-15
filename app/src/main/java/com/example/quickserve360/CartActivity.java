package com.example.quickserve360;

import android.content.Intent; // Add this import
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartList = new ArrayList<>();
    private TextView txtTotal;
    private Button btnGoBack, btnProceedToPayment;
    private DatabaseReference cartRef;
    private double totalAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        txtTotal = findViewById(R.id.txtTotalPrice);
        btnGoBack = findViewById(R.id.btnGoBack);
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment);

        btnGoBack.setOnClickListener(v -> finish());

        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cartList);
        cartRecyclerView.setAdapter(cartAdapter);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cartRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("cart");

        btnProceedToPayment.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            } else {
                proceedToPayment();
            }
        });

        loadCart();
    }

    private void loadCart() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cartList.clear();
                totalAmount = 0; // Reset total
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);
                    if (item != null) {
                        cartList.add(item);
                        totalAmount += item.getPrice() * item.getQuantity();
                    }
                }
                cartAdapter.notifyDataSetChanged();
                txtTotal.setText("Total: ₹" + (int) totalAmount);

                // Enable/disable payment button based on cart items
                btnProceedToPayment.setEnabled(!cartList.isEmpty());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CartActivity.this,
                        "Failed to load cart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToPayment() {
        Intent intent = new Intent(CartActivity.this, PaymentActivity.class);
        intent.putExtra("TOTAL_AMOUNT", totalAmount);
        intent.putExtra("CART_ITEMS_COUNT", cartList.size());
        startActivity(intent);
    }
}