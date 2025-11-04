package com.example.quickserve360;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminStatisticsActivity extends AppCompatActivity {

    private LineChart ordersLineChart, revenueLineChart;
    private BarChart citiesBarChart, restaurantRevenueBarChart, topDishesBarChart, reviewRatingsBarChart;
    private PieChart paymentPieChart, categoryPieChart, userPrefsPieChart, orderStatusPieChart;
    private DatabaseReference databaseReference;
    private ImageButton btnBackArrow;

    private static final String TAG = "AdminStatistics";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_admin_statistics);

        // Initialize back button
        btnBackArrow = findViewById(R.id.btnBackArrow);
        btnBackArrow.setOnClickListener(v -> finish());

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
        orderStatusPieChart = findViewById(R.id.order_status_pie_chart);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        loadStatistics();
    }

    private void loadStatistics() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Starting data processing...");

                // Data structures for analytics
                Map<String, Integer> ordersByDate = new HashMap<>();
                Map<String, Double> revenueByDate = new HashMap<>();
                Map<String, Integer> ordersByCity = new HashMap<>();
                Map<String, Integer> ordersByPayment = new HashMap<>();
                Map<String, Double> revenueByRestaurant = new HashMap<>();
                Map<String, Integer> dishOrderCount = new HashMap<>();
                Map<String, Integer> ordersByCategory = new HashMap<>();
                Map<String, Integer> cuisinePrefs = new HashMap<>();
                Map<String, Float> restaurantRatings = new HashMap<>();
                Map<String, Integer> ordersByStatus = new HashMap<>();

                // Build dish-to-restaurant mapping FIRST
                Map<String, String> dishToRestaurantMap = new HashMap<>();
                Map<String, String> restaurantCategories = new HashMap<>();
                Map<String, String> restaurantLocations = new HashMap<>();
                Map<String, String> restaurantNames = new HashMap<>();

                Log.d(TAG, "Building dish-to-restaurant mapping...");

                // Build mapping from Dishes
                DataSnapshot dishesSnapshot = snapshot.child("Dishes");
                for (DataSnapshot restaurantSnap : dishesSnapshot.getChildren()) {
                    String restaurantId = restaurantSnap.getKey();
                    for (DataSnapshot dishSnap : restaurantSnap.getChildren()) {
                        String dishId = dishSnap.getKey();
                        dishToRestaurantMap.put(dishId, restaurantId);
                        Log.d(TAG, "Mapped dish " + dishId + " to restaurant " + restaurantId);
                    }
                }

                // Build restaurant info mapping
                DataSnapshot restaurantsSnapshot = snapshot.child("Restaurants");
                for (DataSnapshot restaurantSnap : restaurantsSnapshot.getChildren()) {
                    String restaurantId = restaurantSnap.getKey();
                    String name = restaurantSnap.child("name").getValue(String.class);
                    String category = restaurantSnap.child("category").getValue(String.class);
                    String location = restaurantSnap.child("location").getValue(String.class);
                    Float rating = restaurantSnap.child("rating").getValue(Float.class);

                    if (name != null) restaurantNames.put(restaurantId, name);
                    if (category != null) restaurantCategories.put(restaurantId, category);
                    if (location != null) restaurantLocations.put(restaurantId, location);
                    if (name != null && rating != null) restaurantRatings.put(name, rating);

                    Log.d(TAG, "Restaurant: " + name + " | Category: " + category + " | Location: " + location);
                }

                // Process orders
                DataSnapshot ordersSnapshot = snapshot.child("orders");
                int totalOrdersProcessed = 0;
                int totalItemsProcessed = 0;

                if (ordersSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : ordersSnapshot.getChildren()) {
                        for (DataSnapshot orderSnapshot : userSnapshot.getChildren()) {
                            totalOrdersProcessed++;
                            String orderDate = orderSnapshot.child("orderDate").getValue(String.class);
                            String status = orderSnapshot.child("status").getValue(String.class);
                            String paymentMethod = orderSnapshot.child("paymentMethod").getValue(String.class);

                            // Process total amount
                            Object amountObj = orderSnapshot.child("totalAmount").getValue();
                            double totalAmount = 0.0;
                            if (amountObj instanceof Long) {
                                totalAmount = ((Long) amountObj).doubleValue();
                            } else if (amountObj instanceof Double) {
                                totalAmount = (Double) amountObj;
                            } else if (amountObj instanceof Integer) {
                                totalAmount = ((Integer) amountObj).doubleValue();
                            }

                            // Process date
                            if (orderDate != null) {
                                String dateKey = orderDate.split(" ")[0];
                                ordersByDate.put(dateKey, ordersByDate.getOrDefault(dateKey, 0) + 1);
                                revenueByDate.put(dateKey, revenueByDate.getOrDefault(dateKey, 0.0) + totalAmount);
                            }

                            // Process status
                            if (status != null) {
                                ordersByStatus.put(status, ordersByStatus.getOrDefault(status, 0) + 1);
                            }

                            // Process payment method
                            if (paymentMethod != null) {
                                ordersByPayment.put(paymentMethod, ordersByPayment.getOrDefault(paymentMethod, 0) + 1);
                            }

                            // Process order items
                            DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                            Set<String> restaurantsInThisOrder = new HashSet<>();

                            if (itemsSnapshot.exists()) {
                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                    totalItemsProcessed++;
                                    String dishId = itemSnapshot.child("id").getValue(String.class);
                                    String dishName = itemSnapshot.child("name").getValue(String.class);
                                    Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                    if (quantity == null) quantity = 1;

                                    // Track dish popularity
                                    if (dishName != null) {
                                        dishOrderCount.put(dishName, dishOrderCount.getOrDefault(dishName, 0) + quantity);
                                    }

                                    // Find restaurant for this dish
                                    if (dishId != null) {
                                        String restaurantId = dishToRestaurantMap.get(dishId);
                                        if (restaurantId != null) {
                                            restaurantsInThisOrder.add(restaurantId);

                                            String restaurantName = restaurantNames.get(restaurantId);
                                            String category = restaurantCategories.get(restaurantId);
                                            String location = restaurantLocations.get(restaurantId);

                                            // Add revenue to restaurant
                                            if (restaurantName != null) {
                                                revenueByRestaurant.put(restaurantName,
                                                        revenueByRestaurant.getOrDefault(restaurantName, 0.0) + (totalAmount / restaurantsInThisOrder.size()));
                                            }

                                            // Count category for each item
                                            if (category != null) {
                                                ordersByCategory.put(category,
                                                        ordersByCategory.getOrDefault(category, 0) + quantity);
                                                Log.d(TAG, "Added " + quantity + " to category " + category + " from dish " + dishName);
                                            }
                                        } else {
                                            Log.d(TAG, "No restaurant found for dish: " + dishId);
                                        }
                                    }
                                }
                            }

                            // Count cities based on restaurants in this order
                            for (String restaurantId : restaurantsInThisOrder) {
                                String location = restaurantLocations.get(restaurantId);
                                if (location != null) {
                                    ordersByCity.put(location, ordersByCity.getOrDefault(location, 0) + 1);
                                    Log.d(TAG, "Added order count for city: " + location);
                                }
                            }
                        }
                    }
                }

                Log.d(TAG, "Total orders processed: " + totalOrdersProcessed);
                Log.d(TAG, "Total items processed: " + totalItemsProcessed);

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

                // DEBUG: Print all counts
                Log.d(TAG, "=== FINAL COUNTS ===");
                Log.d(TAG, "Orders by Category: " + ordersByCategory);
                Log.d(TAG, "Orders by City: " + ordersByCity);
                Log.d(TAG, "Restaurant Ratings: " + restaurantRatings);
                Log.d(TAG, "Dish Order Count: " + dishOrderCount);
                Log.d(TAG, "Revenue by Restaurant: " + revenueByRestaurant);

                // Show debug info in Toast
                StringBuilder debugInfo = new StringBuilder();
                debugInfo.append("Categories: ").append(ordersByCategory).append("\n");
                debugInfo.append("Cities: ").append(ordersByCity);
                Toast.makeText(AdminStatisticsActivity.this, debugInfo.toString(), Toast.LENGTH_LONG).show();

                // Filter out zero values
                Map<String, Double> filteredRevenue = filterZeroValues(revenueByRestaurant);
                Map<String, Integer> filteredCategories = filterZeroValues(ordersByCategory);

                // Setup all charts
                setupOrdersChart(sortIntegerMapByDate(ordersByDate));
                setupRevenueChart(sortDoubleMapByDate(revenueByDate));
                setupCitiesChart(ordersByCity);
                setupPaymentChart(ordersByPayment);
                setupRestaurantRevenueChart(getTopEntries(filteredRevenue, 8));
                setupTopDishesChart(getTopEntries(dishOrderCount, 10));
                setupCategoryChart(filteredCategories);
                setupUserPrefsChart(cuisinePrefs);
                setupReviewRatingsChart(getTopEntries(restaurantRatings, 8));
                setupOrderStatusChart(ordersByStatus);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(AdminStatisticsActivity.this,
                        "Failed to load statistics: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to filter zero values
    private <T extends Number> Map<String, T> filterZeroValues(Map<String, T> map) {
        Map<String, T> filtered = new HashMap<>();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (entry.getValue().doubleValue() > 0) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    // Helper methods for sorting and filtering
    private Map<String, Integer> sortIntegerMapByDate(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        Collections.sort(entries, (o1, o2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Date date1 = sdf.parse(o1.getKey());
                Date date2 = sdf.parse(o2.getKey());
                return date1.compareTo(date2);
            } catch (Exception e) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private Map<String, Double> sortDoubleMapByDate(Map<String, Double> map) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(map.entrySet());
        Collections.sort(entries, (o1, o2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Date date1 = sdf.parse(o1.getKey());
                Date date2 = sdf.parse(o2.getKey());
                return date1.compareTo(date2);
            } catch (Exception e) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private <T extends Number> Map<String, T> getTopEntries(Map<String, T> map, int limit) {
        List<Map.Entry<String, T>> entries = new ArrayList<>(map.entrySet());
        entries.sort((o1, o2) -> Double.compare(o2.getValue().doubleValue(), o1.getValue().doubleValue()));

        Map<String, T> topEntries = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            topEntries.put(entries.get(i).getKey(), entries.get(i).getValue());
        }
        return topEntries;
    }

    // Chart setup methods
    private void setupOrdersChart(Map<String, Integer> ordersByDate) {
        if (ordersByDate.isEmpty()) {
            ordersLineChart.setNoDataText("No order data available");
            ordersLineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : ordersByDate.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Orders");
        dataSet.setColor(Color.parseColor("#673AB7"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#673AB7"));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#D1C4E9"));
        dataSet.setFillAlpha(80);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        LineData lineData = new LineData(dataSet);
        ordersLineChart.setData(lineData);
        ordersLineChart.getDescription().setText("Daily Orders Growth Trend");
        ordersLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        ordersLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        ordersLineChart.getXAxis().setGranularity(1f);
        ordersLineChart.getXAxis().setLabelRotationAngle(45f);
        ordersLineChart.getAxisLeft().setAxisMinimum(0f);
        ordersLineChart.getAxisRight().setEnabled(false);
        ordersLineChart.getLegend().setEnabled(false);
        ordersLineChart.invalidate();
    }

    private void setupRevenueChart(Map<String, Double> revenueByDate) {
        if (revenueByDate.isEmpty()) {
            revenueLineChart.setNoDataText("No revenue data available");
            revenueLineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : revenueByDate.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#C8E6C9"));
        dataSet.setFillAlpha(80);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "₹" + ((int) value);
            }
        });

        LineData lineData = new LineData(dataSet);
        revenueLineChart.setData(lineData);
        revenueLineChart.getDescription().setText("Revenue Growth Analysis");
        revenueLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        revenueLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        revenueLineChart.getXAxis().setGranularity(1f);
        revenueLineChart.getXAxis().setLabelRotationAngle(45f);
        revenueLineChart.getAxisLeft().setAxisMinimum(0f);
        revenueLineChart.getAxisRight().setEnabled(false);
        revenueLineChart.getLegend().setEnabled(false);
        revenueLineChart.invalidate();
    }

    private void setupCitiesChart(Map<String, Integer> ordersByCity) {
        if (ordersByCity.isEmpty()) {
            citiesBarChart.setNoDataText("No city data available");
            citiesBarChart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : ordersByCity.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Orders");
        dataSet.setColor(Color.parseColor("#FF5722"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        citiesBarChart.setData(barData);
        citiesBarChart.getDescription().setText("Orders Distribution Across Cities");
        citiesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        citiesBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        citiesBarChart.getXAxis().setGranularity(1f);
        citiesBarChart.getAxisLeft().setAxisMinimum(0f);
        citiesBarChart.getAxisRight().setEnabled(false);
        citiesBarChart.getLegend().setEnabled(false);
        citiesBarChart.invalidate();
    }

    private void setupPaymentChart(Map<String, Integer> ordersByPayment) {
        if (ordersByPayment.isEmpty()) {
            paymentPieChart.setNoDataText("No payment data available");
            paymentPieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ordersByPayment.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {
                Color.parseColor("#9C27B0"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#673AB7"),
                Color.parseColor("#00BCD4")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        paymentPieChart.setData(pieData);
        paymentPieChart.getDescription().setText("Payment Method Preferences");
        paymentPieChart.setUsePercentValues(false);
        paymentPieChart.setEntryLabelColor(Color.BLACK);
        paymentPieChart.setEntryLabelTextSize(11f);
        paymentPieChart.getLegend().setTextSize(12f);
        paymentPieChart.setDrawHoleEnabled(true);
        paymentPieChart.setHoleRadius(40f);
        paymentPieChart.setTransparentCircleRadius(45f);
        paymentPieChart.invalidate();
    }

    private void setupRestaurantRevenueChart(Map<String, Double> revenueByRestaurant) {
        if (revenueByRestaurant.isEmpty()) {
            restaurantRevenueBarChart.setNoDataText("No restaurant revenue data available");
            restaurantRevenueBarChart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : revenueByRestaurant.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Revenue");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "₹" + ((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        restaurantRevenueBarChart.setData(barData);
        restaurantRevenueBarChart.getDescription().setText("Top Revenue Generating Restaurants");
        restaurantRevenueBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        restaurantRevenueBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        restaurantRevenueBarChart.getXAxis().setGranularity(1f);
        restaurantRevenueBarChart.getXAxis().setLabelRotationAngle(45f);
        restaurantRevenueBarChart.getAxisLeft().setAxisMinimum(0f);
        restaurantRevenueBarChart.getAxisRight().setEnabled(false);
        restaurantRevenueBarChart.getLegend().setEnabled(false);
        restaurantRevenueBarChart.invalidate();
    }

    private void setupTopDishesChart(Map<String, Integer> dishOrderCount) {
        if (dishOrderCount.isEmpty()) {
            topDishesBarChart.setNoDataText("No dish order data available");
            topDishesBarChart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : dishOrderCount.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Quantity Sold");
        dataSet.setColor(Color.parseColor("#FFC107"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        topDishesBarChart.setData(barData);
        topDishesBarChart.getDescription().setText("Best Selling Menu Items");
        topDishesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        topDishesBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        topDishesBarChart.getXAxis().setGranularity(1f);
        topDishesBarChart.getXAxis().setLabelRotationAngle(45f);
        topDishesBarChart.getAxisLeft().setAxisMinimum(0f);
        topDishesBarChart.getAxisRight().setEnabled(false);
        topDishesBarChart.getLegend().setEnabled(false);
        topDishesBarChart.invalidate();
    }

    private void setupCategoryChart(Map<String, Integer> ordersByCategory) {
        if (ordersByCategory.isEmpty()) {
            categoryPieChart.setNoDataText("No category data available");
            categoryPieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ordersByCategory.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            categoryPieChart.setNoDataText("No category data available");
            categoryPieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {
                Color.parseColor("#FF5722"),
                Color.parseColor("#673AB7"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#FF9800")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        categoryPieChart.setData(pieData);
        categoryPieChart.getDescription().setText("Most Ordered Categories");
        categoryPieChart.setUsePercentValues(false);
        categoryPieChart.setEntryLabelColor(Color.BLACK);
        categoryPieChart.setEntryLabelTextSize(11f);
        categoryPieChart.getLegend().setTextSize(12f);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleRadius(40f);
        categoryPieChart.setTransparentCircleRadius(45f);
        categoryPieChart.invalidate();
    }

    private void setupUserPrefsChart(Map<String, Integer> cuisinePrefs) {
        if (cuisinePrefs.isEmpty()) {
            userPrefsPieChart.setNoDataText("No preference data available");
            userPrefsPieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cuisinePrefs.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {
                Color.parseColor("#9C27B0"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#673AB7"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#E91E63")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        userPrefsPieChart.setData(pieData);
        userPrefsPieChart.getDescription().setText("User Cuisine Preferences");
        userPrefsPieChart.setUsePercentValues(false);
        userPrefsPieChart.setEntryLabelColor(Color.BLACK);
        userPrefsPieChart.setEntryLabelTextSize(11f);
        userPrefsPieChart.getLegend().setTextSize(12f);
        userPrefsPieChart.setDrawHoleEnabled(true);
        userPrefsPieChart.setHoleRadius(40f);
        userPrefsPieChart.setTransparentCircleRadius(45f);
        userPrefsPieChart.invalidate();
    }

    private void setupReviewRatingsChart(Map<String, Float> restaurantRatings) {
        if (restaurantRatings.isEmpty()) {
            reviewRatingsBarChart.setNoDataText("No rating data available");
            reviewRatingsBarChart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : restaurantRatings.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Average Rating");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        reviewRatingsBarChart.setData(barData);
        reviewRatingsBarChart.getDescription().setText("Restaurant Performance Ratings");
        reviewRatingsBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        reviewRatingsBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        reviewRatingsBarChart.getXAxis().setGranularity(1f);
        reviewRatingsBarChart.getXAxis().setLabelRotationAngle(45f);
        reviewRatingsBarChart.getAxisLeft().setAxisMinimum(0f);
        reviewRatingsBarChart.getAxisLeft().setAxisMaximum(5f);
        reviewRatingsBarChart.getAxisRight().setEnabled(false);
        reviewRatingsBarChart.getLegend().setEnabled(false);
        reviewRatingsBarChart.invalidate();
    }

    private void setupOrderStatusChart(Map<String, Integer> ordersByStatus) {
        if (ordersByStatus.isEmpty()) {
            orderStatusPieChart.setNoDataText("No order status data available");
            orderStatusPieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ordersByStatus.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        // Color coding: Green for Delivered, Yellow for Out for Delivery, Red for Pending
        int[] colors = {
                Color.parseColor("#4CAF50"), // Delivered - Green
                Color.parseColor("#FF9800"), // Out for Delivery - Orange
                Color.parseColor("#F44336"), // Pending - Red
                Color.parseColor("#2196F3")  // Other statuses - Blue
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        orderStatusPieChart.setData(pieData);
        orderStatusPieChart.getDescription().setText("Order Status Distribution");
        orderStatusPieChart.setUsePercentValues(false);
        orderStatusPieChart.setEntryLabelColor(Color.BLACK);
        orderStatusPieChart.setEntryLabelTextSize(11f);
        orderStatusPieChart.getLegend().setTextSize(12f);
        orderStatusPieChart.setDrawHoleEnabled(true);
        orderStatusPieChart.setHoleRadius(40f);
        orderStatusPieChart.setTransparentCircleRadius(45f);
        orderStatusPieChart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}