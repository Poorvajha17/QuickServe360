package com.example.quickserve360;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class RestaurantAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Restaurant> restaurants;

    public RestaurantAdapter(Context context, ArrayList<Restaurant> restaurants) {
        this.context = context;
        this.restaurants = restaurants;
    }

    @Override
    public int getCount() {
        return restaurants.size();
    }

    @Override
    public Object getItem(int position) {
        return restaurants.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_restaurant, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.txtRestName);
            holder.desc = convertView.findViewById(R.id.txtRestDesc);
            holder.ratingText = convertView.findViewById(R.id.txtRestRating); // changed
            holder.image = convertView.findViewById(R.id.imgRest);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get current restaurant
        Restaurant r = restaurants.get(position);

        holder.name.setText(r.getName() != null ? r.getName() : "No Name");
        holder.desc.setText(r.getDescription() != null ? r.getDescription() : "");

        // Display rating value as text (like "4.5 ★")
        holder.ratingText.setText(String.format("%.1f ★", r.getRating()));

        // Load image with Glide
        Glide.with(context)
                .load(r.getImagePath() != null ? r.getImagePath() : "")
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.image);

        return convertView;
    }

    static class ViewHolder {
        TextView name, desc, ratingText; // changed
        ImageView image;
    }
}