package com.example.quickserve360;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ManageMenuActivity extends AppCompatActivity {

    private RecyclerView menuRecyclerView;
    private FloatingActionButton addDishFab;
    private DatabaseReference databaseReference;
    private MenuAdapter adapter;
    private List<Dish> dishList;
    private String restaurantId, restaurantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_menu);

        restaurantId = getIntent().getStringExtra("restaurantId");
        restaurantName = getIntent().getStringExtra("restaurantName");

        getSupportActionBar().setTitle("Menu - " + restaurantName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        menuRecyclerView = findViewById(R.id.menu_recycler_view);
        addDishFab = findViewById(R.id.add_dish_fab);

        databaseReference = FirebaseDatabase.getInstance().getReference("Dishes").child(restaurantId);

        dishList = new ArrayList<>();
        adapter = new MenuAdapter(dishList);

        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuRecyclerView.setAdapter(adapter);

        loadMenu();

        addDishFab.setOnClickListener(v -> showAddDishDialog());
    }

    private void loadMenu() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dishList.clear();
                for (DataSnapshot dishSnapshot : snapshot.getChildren()) {
                    Dish dish = dishSnapshot.getValue(Dish.class);
                    if (dish != null) {
                        dishList.add(dish);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageMenuActivity.this,
                        "Failed to load menu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDishDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_dish, null);
        builder.setView(dialogView);

        EditText nameEdit = dialogView.findViewById(R.id.dish_name_edit);
        EditText descEdit = dialogView.findViewById(R.id.dish_description_edit);
        EditText priceEdit = dialogView.findViewById(R.id.dish_price_edit);
        EditText imageEdit = dialogView.findViewById(R.id.dish_image_edit);
        Button addButton = dialogView.findViewById(R.id.add_dish_button);

        AlertDialog dialog = builder.create();

        addButton.setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            String description = descEdit.getText().toString().trim();
            String priceStr = priceEdit.getText().toString().trim();
            String image = imageEdit.getText().toString().trim();

            if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int price = Integer.parseInt(priceStr);
            String dishId = "dish" + System.currentTimeMillis();

            HashMap<String, Object> dishMap = new HashMap<>();
            dishMap.put("id", dishId);
            dishMap.put("name", name);
            dishMap.put("description", description);
            dishMap.put("price", price);
            dishMap.put("imagePath", image);

            databaseReference.child(dishId).setValue(dishMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Dish added successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add dish", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Dish Model
    public static class Dish {
        public String id;
        public String name;
        public String description;
        public int price;
        public String imagePath;

        public Dish() {}
    }

    // Adapter
    class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

        private List<Dish> dishes;

        public MenuAdapter(List<Dish> dishes) {
            this.dishes = dishes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dish_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Dish dish = dishes.get(position);

            holder.nameText.setText(dish.name);
            holder.descText.setText(dish.description);
            holder.priceText.setText("₹" + dish.price);

            Picasso.get().load(dish.imagePath).into(holder.imageView);

            holder.editButton.setOnClickListener(v -> {
                showEditDishDialog(dish);
            });

            holder.deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(ManageMenuActivity.this)
                        .setTitle("Delete Dish")
                        .setMessage("Are you sure you want to delete " + dish.name + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            databaseReference.child(dish.id).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ManageMenuActivity.this,
                                                "Dish deleted", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return dishes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView nameText, descText, priceText;
            Button editButton, deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.dish_image);
                nameText = itemView.findViewById(R.id.dish_name);
                descText = itemView.findViewById(R.id.dish_description);
                priceText = itemView.findViewById(R.id.dish_price);
                editButton = itemView.findViewById(R.id.edit_dish_button);
                deleteButton = itemView.findViewById(R.id.delete_dish_button);
            }
        }
    }

    private void showEditDishDialog(Dish dish) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_dish, null);
        builder.setView(dialogView);

        EditText nameEdit = dialogView.findViewById(R.id.dish_name_edit);
        EditText descEdit = dialogView.findViewById(R.id.dish_description_edit);
        EditText priceEdit = dialogView.findViewById(R.id.dish_price_edit);
        EditText imageEdit = dialogView.findViewById(R.id.dish_image_edit);
        Button addButton = dialogView.findViewById(R.id.add_dish_button);

        // Pre-fill with existing data
        nameEdit.setText(dish.name);
        descEdit.setText(dish.description);
        priceEdit.setText(String.valueOf(dish.price));
        imageEdit.setText(dish.imagePath);
        addButton.setText("Update Dish");

        AlertDialog dialog = builder.create();

        addButton.setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            String description = descEdit.getText().toString().trim();
            String priceStr = priceEdit.getText().toString().trim();
            String image = imageEdit.getText().toString().trim();

            if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int price = Integer.parseInt(priceStr);

            HashMap<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("description", description);
            updates.put("price", price);
            updates.put("imagePath", image);

            databaseReference.child(dish.id).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Dish updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update dish", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}