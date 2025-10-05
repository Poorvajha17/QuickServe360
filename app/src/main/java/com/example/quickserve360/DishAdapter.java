package com.example.quickserve360;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class DishAdapter extends RecyclerView.Adapter<DishAdapter.DishViewHolder> {

    private Context context;
    private ArrayList<Dish> dishList;

    public DishAdapter(Context context, ArrayList<Dish> dishList) {
        this.context = context;
        this.dishList = dishList;
    }

    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dish, parent, false);
        return new DishViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DishViewHolder holder, int position) {
        Dish dish = dishList.get(position);

        holder.txtDishName.setText(dish.getName());
        holder.txtDishDesc.setText(dish.getDescription());
        holder.txtDishPrice.setText("₹" + (int) dish.getPrice());

        Glide.with(context)
                .load(dish.getImagePath())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imgDish);

        holder.btnAddToCart.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (userId == null) {
                Toast.makeText(context, "Please log in to add items to cart", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference cartRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("cart")
                    .child(dish.getId());

            cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    int quantity = 1;
                    if (snapshot.exists()) {
                        CartItem existingItem = snapshot.getValue(CartItem.class);
                        if (existingItem != null) {
                            quantity = existingItem.getQuantity() + 1;
                        }
                    }

                    CartItem cartItem = new CartItem(
                            dish.getId(),
                            dish.getName(),
                            dish.getPrice(),
                            dish.getImagePath(),
                            dish.getDescription(),
                            quantity
                    );

                    cartRef.setValue(cartItem)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, dish.getName() + " added to cart", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to add to cart", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(context, "Failed to access cart", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return dishList.size();
    }

    static class DishViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDish;
        TextView txtDishName, txtDishDesc, txtDishPrice;
        Button btnAddToCart;

        public DishViewHolder(@NonNull View itemView) {
            super(itemView);
            imgDish = itemView.findViewById(R.id.imgDish);
            txtDishName = itemView.findViewById(R.id.txtDishName);
            txtDishDesc = itemView.findViewById(R.id.txtDishDesc);
            txtDishPrice = itemView.findViewById(R.id.txtDishPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}