package com.example.quickserve360;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;

    public CartAdapter(List<CartItem> cartList) {
        this.cartList = cartList;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.txtName.setText(item.getName());
        holder.txtPrice.setText("₹" + (int) item.getPrice());
        holder.txtQuantity.setText(String.valueOf(item.getQuantity()));
        holder.txtItemTotal.setText("Total: ₹" + (int) (item.getPrice() * item.getQuantity()));

        Glide.with(holder.itemView.getContext())
                .load(item.getImagePath())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgDish);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference itemRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("cart")
                .child(item.getId());

        // Increase quantity
        holder.btnIncrease.setOnClickListener(v -> {
            int qty = item.getQuantity() + 1;
            item.setQuantity(qty);
            holder.txtQuantity.setText(String.valueOf(qty));
            holder.txtItemTotal.setText("Total: ₹" + (int) (item.getPrice() * qty));
            itemRef.setValue(item);
        });

        // Decrease quantity
        holder.btnDecrease.setOnClickListener(v -> {
            int qty = item.getQuantity() - 1;
            if (qty <= 0) {
                itemRef.removeValue();   // remove item if qty goes to zero
            } else {
                item.setQuantity(qty);
                holder.txtQuantity.setText(String.valueOf(qty));
                holder.txtItemTotal.setText("Total: ₹" + (int) (item.getPrice() * qty));
                itemRef.setValue(item);
            }
        });

        // Remove item completely
        holder.btnRemove.setOnClickListener(v -> itemRef.removeValue());
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDish;
        TextView txtName, txtPrice, txtQuantity, txtItemTotal;
        Button btnIncrease, btnDecrease, btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgDish      = itemView.findViewById(R.id.imgCartDish);
            txtName      = itemView.findViewById(R.id.txtCartDishName);
            txtPrice     = itemView.findViewById(R.id.txtCartDishPrice);
            txtQuantity  = itemView.findViewById(R.id.txtCartQuantity);
            txtItemTotal = itemView.findViewById(R.id.txtCartItemTotal);
            btnIncrease  = itemView.findViewById(R.id.btnIncreaseQty);
            btnDecrease  = itemView.findViewById(R.id.btnDecreaseQty);
            btnRemove    = itemView.findViewById(R.id.btnRemoveFromCart);
        }
    }
}