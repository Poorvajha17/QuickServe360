package com.example.quickserve360;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminStatisticsActivity extends AppCompatActivity {

    private LineChart ordersLineChart, revenueLineChart;
    private BarChart citiesBarChart, restaurantRevenueBarChart, topDishesBarChart, reviewRatingsBarChart;
    private PieChart paymentPieChart, categoryPieChart, userPrefsPieChart;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_statistics);

        getSupportActionBar().setTitle("Statistics & Analytics");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize charts
        ordersLineChart = findViewById(R.id.orders_line_chart);
        revenueLineChart = findViewById(R.id.revenue_line_chart);
        citiesBarChart = findViewById(R.id.cities_bar_chart);
        paymentPieChart = findViewById(R.id.payment_pie_chart);
        restaurantRevenueBarChart = findViewById(R.id.restaurant_revenue_bar_chart);
        topDishesBarChart = findViewById(R.id.top_dishes_bar_chart);
        categoryPieChart = findViewById(R.id.category_pie_chart);
        reviewRatingsBarChart = findViewById(R.id.review_ratings_bar_chart);
        userPrefsPieChart = findViewById(R.id.user_prefs_pie_chart);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        loadStatistics();
    }

    private void loadStatistics() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> ordersByDate = new HashMap<>();
                Map<String, Double> revenueByDate = new HashMap<>();
                Map<String, Integer> ordersByCity = new HashMap<>();
                Map<String, Integer> ordersByPayment = new HashMap<>();
                Map<String, Double> revenueByRestaurant = new HashMap<>();
                Map<String, Integer> dishOrderCount = new HashMap<>();
                Map<String, Integer> ordersByCategory = new HashMap<>();
                Map<String, Integer> cuisinePrefs = new HashMap<>();
                Map<String, Float> restaurantRatings = new HashMap<>();

                // Process orders
                DataSnapshot ordersSnapshot = snapshot.child("orders");
                if (ordersSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : ordersSnapshot.getChildren()) {
                        for (DataSnapshot orderSnapshot : userSnapshot.getChildren()) {
                            String orderDate = orderSnapshot.child("orderDate").getValue(String.class);
                            if (orderDate != null) {
                                String dateKey = orderDate.substring(0, Math.min(5, orderDate.length()));
                                ordersByDate.put(dateKey, ordersByDate.getOrDefault(dateKey, 0) + 1);
                            }

                            Double totalAmount = orderSnapshot.child("totalAmount").getValue(Double.class);
                            if (totalAmount != null && orderDate != null) {
                                String dateKey = orderDate.substring(0, Math.min(5, orderDate.length()));
                                revenueByDate.put(dateKey, revenueByDate.getOrDefault(dateKey, 0.0) + totalAmount);
                            }

                            String paymentMethod = orderSnapshot.child("paymentMethod").getValue(String.class);
                            if (paymentMethod != null) {
                                ordersByPayment.put(paymentMethod, ordersByPayment.getOrDefault(paymentMethod, 0) + 1);
                            }

                            for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                                String dishId = itemSnapshot.child("id").getValue(String.class);
                                Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                if (dishId != null && quantity != null) {
                                    String dishKey = dishId + "_" + userSnapshot.getKey() + "_" + orderSnapshot.getKey();
                                    dishOrderCount.put(dishKey, dishOrderCount.getOrDefault(dishKey, 0) + quantity);

                                    for (DataSnapshot restSnapshot : snapshot.child("Dishes").getChildren()) {
                                        if (restSnapshot.hasChild(dishId)) {
                                            String restId = restSnapshot.getKey();
                                            DataSnapshot restData = snapshot.child("Restaurants").child(restId);
                                            String restName = restData.child("name").getValue(String.class);
                                            String category = restData.child("category").getValue(String.class);
                                            if (restName != null && totalAmount != null) {
                                                revenueByRestaurant.put(restName, revenueByRestaurant.getOrDefault(restName, 0.0) + totalAmount);
                                            }
                                            if (category != null) {
                                                ordersByCategory.put(category, ordersByCategory.getOrDefault(category, 0) + quantity);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Process users
                DataSnapshot usersSnapshot = snapshot.child("users");
                if (usersSnapshot.exists()) {
                    for (DataSnapshot user : usersSnapshot.getChildren()) {
                        String city = user.child("selectedCity").getValue(String.class);
                        if (city != null) {
                            ordersByCity.put(city, ordersByCity.getOrDefault(city, 0) + 1);
                        }
                    }
                }

                // Process user preferences
                DataSnapshot prefsSnapshot = snapshot.child("UserPreferences");
                if (prefsSnapshot.exists()) {
                    for (DataSnapshot pref : prefsSnapshot.getChildren()) {
                        String cuisine = pref.child("cuisine").getValue(String.class);
                        if (cuisine != null) {
                            cuisinePrefs.put(cuisine, cuisinePrefs.getOrDefault(cuisine, 0) + 1);
                        }
                    }
                }

                // Process reviews
                DataSnapshot reviewsSnapshot = snapshot.child("Reviews");
                if (reviewsSnapshot.exists()) {
                    Map<String, List<Float>> ratingsByRestaurant = new HashMap<>();
                    for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                        String restId = review.child("restaurantId").getValue(String.class);
                        Float rating = review.child("rating").getValue(Float.class);
                        if (restId != null && rating != null) {
                            String restName = snapshot.child("Restaurants").child(restId).child("name").getValue(String.class);
                            if (restName != null) {
                                ratingsByRestaurant.computeIfAbsent(restName, k -> new ArrayList<>()).add(rating);
                            }
                        }
                    }
                    for (Map.Entry<String, List<Float>> entry : ratingsByRestaurant.entrySet()) {
                        List<Float> ratings = entry.getValue();
                        if (!ratings.isEmpty()) {
                            float avgRating = (float) ratings.stream().mapToDouble(Float::doubleValue).average().getAsDouble();
                            restaurantRatings.put(entry.getKey(), avgRating);
                        }
                    }
                }

                // Setup charts
                setupOrdersChart(ordersByDate);
                setupRevenueChart(revenueByDate);
                setupCitiesChart(ordersByCity);
                setupPaymentChart(ordersByPayment);
                setupRestaurantRevenueChart(revenueByRestaurant);
                setupTopDishesChart(dishOrderCount, snapshot);
                setupCategoryChart(ordersByCategory);
                setupUserPrefsChart(cuisinePrefs);
                setupReviewRatingsChart(restaurantRatings);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminStatisticsActivity.this, "Failed to load statistics: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupOrdersChart(Map<String, Integer> ordersByDate) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : ordersByDate.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Orders Over Time");
        dataSet.setColor(Color.parseColor("#673AB7"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#673AB7"));
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        ordersLineChart.setData(lineData);
        ordersLineChart.getDescription().setText("Daily Orders");
        ordersLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        ordersLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        ordersLineChart.getXAxis().setGranularity(1f);
        ordersLineChart.getAxisLeft().setAxisMinimum(0f);
        ordersLineChart.invalidate();
    }

    private void setupRevenueChart(Map<String, Double> revenueByDate) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : revenueByDate.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue Over Time");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        revenueLineChart.setData(lineData);
        revenueLineChart.getDescription().setText("Daily Revenue (₹)");
        revenueLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        revenueLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        revenueLineChart.getXAxis().setGranularity(1f);
        revenueLineChart.getAxisLeft().setAxisMinimum(0f);
        revenueLineChart.invalidate();
    }

    private void setupCitiesChart(Map<String, Integer> ordersByCity) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : ordersByCity.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Users by City");
        dataSet.setColor(Color.parseColor("#FF5722"));
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        citiesBarChart.setData(barData);
        citiesBarChart.getDescription().setText("User Distribution by City");
        citiesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        citiesBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        citiesBarChart.getXAxis().setGranularity(1f);
        citiesBarChart.getAxisLeft().setAxisMinimum(0f);
        citiesBarChart.invalidate();
    }

    private void setupPaymentChart(Map<String, Integer> ordersByPayment) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ordersByPayment.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Payment Methods");
        int[] colors = {Color.parseColor("#673AB7"), Color.parseColor("#4CAF50"), Color.parseColor("#FF5722"), Color.parseColor("#2196F3")};
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        paymentPieChart.setData(pieData);
        paymentPieChart.getDescription().setText("Payment Method Distribution");
        paymentPieChart.setUsePercentValues(true);
        paymentPieChart.invalidate();
    }

    private void setupRestaurantRevenueChart(Map<String, Double> revenueByRestaurant) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : revenueByRestaurant.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Revenue by Restaurant");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        restaurantRevenueBarChart.setData(barData);
        restaurantRevenueBarChart.getDescription().setText("Revenue by Restaurant (₹)");
        restaurantRevenueBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        restaurantRevenueBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        restaurantRevenueBarChart.getXAxis().setGranularity(1f);
        restaurantRevenueBarChart.getXAxis().setLabelRotationAngle(45f);
        restaurantRevenueBarChart.getAxisLeft().setAxisMinimum(0f);
        restaurantRevenueBarChart.invalidate();
    }

    private void setupTopDishesChart(Map<String, Integer> dishOrderCount, DataSnapshot snapshot) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        Map<String, Integer> dishTotals = new HashMap<>();
        for (Map.Entry<String, Integer> entry : dishOrderCount.entrySet()) {
            String dishId = entry.getKey().split("_")[0];
            for (DataSnapshot restSnapshot : snapshot.child("Dishes").getChildren()) {
                if (restSnapshot.hasChild(dishId)) {
                    String dishName = restSnapshot.child(dishId).child("name").getValue(String.class);
                    if (dishName != null) {
                        dishTotals.put(dishName, dishTotals.getOrDefault(dishName, 0) + entry.getValue());
                    }
                    break;
                }
            }
        }

        for (Map.Entry<String, Integer> entry : dishTotals.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Top Dishes by Quantity");
        dataSet.setColor(Color.parseColor("#FFC107"));
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        topDishesBarChart.setData(barData);
        topDishesBarChart.getDescription().setText("Top-Selling Dishes");
        topDishesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        topDishesBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        topDishesBarChart.getXAxis().setGranularity(1f);
        topDishesBarChart.getXAxis().setLabelRotationAngle(45f);
        topDishesBarChart.getAxisLeft().setAxisMinimum(0f);
        topDishesBarChart.invalidate();
    }

    private void setupCategoryChart(Map<String, Integer> ordersByCategory) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ordersByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Orders by Category");
        int[] colors = {Color.parseColor("#FF5722"), Color.parseColor("#673AB7"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#FFC107")};
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        categoryPieChart.setData(pieData);
        categoryPieChart.getDescription().setText("Category Popularity");
        categoryPieChart.setUsePercentValues(true);
        categoryPieChart.invalidate();
    }

    private void setupUserPrefsChart(Map<String, Integer> cuisinePrefs) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cuisinePrefs.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "User Cuisine Preferences");
        int[] colors = {Color.parseColor("#9C27B0"), Color.parseColor("#4CAF50"), Color.parseColor("#FF5722"), Color.parseColor("#2196F3")};
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        userPrefsPieChart.setData(pieData);
        userPrefsPieChart.getDescription().setText("Cuisine Preferences");
        userPrefsPieChart.setUsePercentValues(true);
        userPrefsPieChart.invalidate();
    }

    private void setupReviewRatingsChart(Map<String, Float> restaurantRatings) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : restaurantRatings.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Average Restaurant Ratings");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        reviewRatingsBarChart.setData(barData);
        reviewRatingsBarChart.getDescription().setText("Restaurant Ratings");
        reviewRatingsBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        reviewRatingsBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        reviewRatingsBarChart.getXAxis().setGranularity(1f);
        reviewRatingsBarChart.getXAxis().setLabelRotationAngle(45f);
        reviewRatingsBarChart.getAxisLeft().setAxisMinimum(0f);
        reviewRatingsBarChart.getAxisLeft().setAxisMaximum(5f);
        reviewRatingsBarChart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}