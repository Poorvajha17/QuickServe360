package com.example.quickserve360;

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
    private Button btnGoBack;

    private DatabaseReference cartRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        txtTotal = findViewById(R.id.txtTotalPrice);
        btnGoBack = findViewById(R.id.btnGoBack);

        btnGoBack.setOnClickListener(v -> finish());

        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cartList);
        cartRecyclerView.setAdapter(cartAdapter);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cartRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("cart");

        loadCart();
    }

    private void loadCart() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cartList.clear();
                double total = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);
                    if (item != null) {
                        cartList.add(item);
                        total += item.getPrice() * item.getQuantity();
                    }
                }
                cartAdapter.notifyDataSetChanged();
                txtTotal.setText("Total: ₹" + (int) total);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CartActivity.this,
                        "Failed to load cart", Toast.LENGTH_SHORT).show();
            }
        });
    }
}