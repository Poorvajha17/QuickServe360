package com.example.quickserve360;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageOrdersActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private DatabaseReference ordersReference;
    private OrderAdapter adapter;
    private List<OrderInfo> orderList;
    private ImageButton btnBackArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_manage_orders);

        // Initialize back button
        btnBackArrow = findViewById(R.id.btnBackArrow);
        btnBackArrow.setOnClickListener(v -> finish());

        ordersRecyclerView = findViewById(R.id.orders_recycler_view);
        ordersReference = FirebaseDatabase.getInstance().getReference("orders");

        orderList = new ArrayList<>();
        adapter = new OrderAdapter(orderList);

        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        ordersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    for (DataSnapshot orderSnapshot : userSnapshot.getChildren()) {
                        OrderInfo orderInfo = new OrderInfo();
                        orderInfo.userId = userId;
                        orderInfo.orderId = orderSnapshot.child("orderId").getValue(String.class);
                        orderInfo.orderDate = orderSnapshot.child("orderDate").getValue(String.class);
                        orderInfo.status = orderSnapshot.child("status").getValue(String.class);
                        orderInfo.totalAmount = orderSnapshot.child("totalAmount").getValue(Double.class);
                        orderInfo.paymentMethod = orderSnapshot.child("paymentMethod").getValue(String.class);

                        int itemCount = 0;
                        for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                            itemCount++;
                        }
                        orderInfo.itemCount = itemCount;

                        orderList.add(orderInfo);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageOrdersActivity.this,
                        "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Order Info Model
    public static class OrderInfo {
        public String userId;
        public String orderId;
        public String orderDate;
        public String status;
        public Double totalAmount;
        public String paymentMethod;
        public int itemCount;
    }

    // Adapter Class
    class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

        private List<OrderInfo> orders;

        public OrderAdapter(List<OrderInfo> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OrderInfo order = orders.get(position);

            holder.orderIdText.setText("Order #" + order.orderId);
            holder.orderDateText.setText(order.orderDate);
            holder.statusText.setText("Status: " + order.status);
            holder.amountText.setText("₹" + String.format("%.2f", order.totalAmount));
            holder.itemCountText.setText(order.itemCount + " items");
            holder.paymentText.setText(order.paymentMethod);

            // Set status color
            if ("Pending".equals(order.status)) {
                holder.statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else if ("Preparing".equals(order.status)) {
                holder.statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else if ("Out for Delivery".equals(order.status)) {
                holder.statusText.setTextColor(getResources().getColor(android.R.color.holo_purple));
            } else if ("Delivered".equals(order.status)) {
                holder.statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if ("Cancelled".equals(order.status)) {
                holder.statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            holder.updateStatusButton.setOnClickListener(v -> {
                showUpdateStatusDialog(order);
            });
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView orderIdText, orderDateText, statusText, amountText, itemCountText, paymentText;
            Button updateStatusButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                orderIdText = itemView.findViewById(R.id.order_id_text);
                orderDateText = itemView.findViewById(R.id.order_date_text);
                statusText = itemView.findViewById(R.id.order_status_text);
                amountText = itemView.findViewById(R.id.order_amount_text);
                itemCountText = itemView.findViewById(R.id.order_item_count_text);
                paymentText = itemView.findViewById(R.id.order_payment_text);
                updateStatusButton = itemView.findViewById(R.id.update_status_button);
            }
        }
    }

    private void showUpdateStatusDialog(OrderInfo order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_order_status, null);
        builder.setView(dialogView);

        Spinner statusSpinner = dialogView.findViewById(R.id.status_spinner);
        Button updateButton = dialogView.findViewById(R.id.update_button);

        String[] statuses = {"Pending", "Preparing", "Out for Delivery", "Delivered", "Cancelled"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        // Set current status
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(order.status)) {
                statusSpinner.setSelection(i);
                break;
            }
        }

        AlertDialog dialog = builder.create();

        updateButton.setOnClickListener(v -> {
            String newStatus = statusSpinner.getSelectedItem().toString();

            ordersReference.child(order.userId).child(order.orderId).child("status")
                    .setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ManageOrdersActivity.this,
                                "Order status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ManageOrdersActivity.this,
                                "Failed to update status", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}