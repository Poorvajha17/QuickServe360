package com.example.quickserve360;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RestaurantListAdapter extends RecyclerView.Adapter<RestaurantListAdapter.ViewHolder> {

    private List<Restaurant> restaurantList;
    private OnRestaurantClickListener listener;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public RestaurantListAdapter(List<Restaurant> restaurantList, OnRestaurantClickListener listener) {
        this.restaurantList = restaurantList;
        this.listener = listener;
        Log.d("RestaurantAdapter", "Adapter created with " + restaurantList.size() + " restaurants");
        Log.d("RestaurantAdapter", "Listener is null: " + (listener == null));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant_list, parent, false);
        Log.d("RestaurantAdapter", "ViewHolder created");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);

        Log.d("RestaurantAdapter", "Binding position " + position + ": " + restaurant.getName());

        // Set restaurant data
        holder.tvRestaurantName.setText(restaurant.getName());
        holder.tvCuisine.setText(restaurant.getCuisine());
        holder.tvLocation.setText(restaurant.getLocation());
        holder.tvRating.setText(String.valueOf(restaurant.getRating()));

        // Load restaurant image using Glide
        if (restaurant.getImagePath() != null && !restaurant.getImagePath().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(restaurant.getImagePath())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(holder.ivRestaurantImage);
        } else {
            // If no image path, set placeholder
            holder.ivRestaurantImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set click listener with logging
        holder.itemView.setOnClickListener(v -> {
            Log.d("RestaurantAdapter", "=== CLICK DETECTED ===");
            Log.d("RestaurantAdapter", "Restaurant clicked: " + restaurant.getName());
            Log.d("RestaurantAdapter", "Restaurant ID: " + restaurant.getId());
            Log.d("RestaurantAdapter", "Position: " + position);
            Log.d("RestaurantAdapter", "Listener is null: " + (listener == null));

            if (listener != null) {
                Log.d("RestaurantAdapter", "Calling listener.onRestaurantClick()");
                listener.onRestaurantClick(restaurant);
            } else {
                Log.e("RestaurantAdapter", "Listener is null! Cannot open review page");
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRestaurantImage;
        TextView tvRestaurantName, tvCuisine, tvLocation, tvRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRestaurantImage = itemView.findViewById(R.id.ivRestaurantImage);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvCuisine = itemView.findViewById(R.id.tvCuisine);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvRating = itemView.findViewById(R.id.tvRating);

            Log.d("RestaurantAdapter", "ViewHolder - All views found: " +
                    (ivRestaurantImage != null && tvRestaurantName != null &&
                            tvCuisine != null && tvLocation != null && tvRating != null));
        }
    }
}