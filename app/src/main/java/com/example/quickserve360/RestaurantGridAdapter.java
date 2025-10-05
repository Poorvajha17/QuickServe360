package com.example.quickserve360;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;

public class RestaurantGridAdapter extends RecyclerView.Adapter<RestaurantGridAdapter.ViewHolder> {

    private ArrayList<Restaurant> items;
    private Context context;
    private OnRestaurantClickListener listener;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public RestaurantGridAdapter(ArrayList<Restaurant> items, OnRestaurantClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RestaurantGridAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View inflate = LayoutInflater.from(context).inflate(R.layout.activity_restaurant_grid_adapter, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantGridAdapter.ViewHolder holder, int position) {
        Restaurant restaurant = items.get(position);

        holder.tvName.setText(restaurant.getName());
        holder.tvCuisine.setText(restaurant.getCuisine());
        holder.tvRating.setText(String.valueOf(restaurant.getRating()));

        Glide.with(context)
                .load(restaurant.getImagePath())
                .transform(new CenterCrop(), new RoundedCorners(20))
                .into(holder.ivRestaurant);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestaurantClick(restaurant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCuisine, tvRating;
        ImageView ivRestaurant;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRestaurantName);
            tvCuisine = itemView.findViewById(R.id.tvCuisine);
            tvRating = itemView.findViewById(R.id.tvRating);
            ivRestaurant = itemView.findViewById(R.id.ivRestaurant);
        }
    }
}