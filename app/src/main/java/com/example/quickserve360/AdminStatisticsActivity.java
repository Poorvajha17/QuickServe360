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
    private BarChart topDishesBarChart, reviewRatingsBarChart;
    private PieChart paymentPieChart, userPrefsPieChart, orderStatusPieChart;
    private DatabaseReference databaseReference;
    private ImageButton btnBackArrow;

    private static final String TAG = "AdminStatistics";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding action bar: " + e.getMessage());
        }
        setContentView(R.layout.activity_admin_statistics);

        try {
            initializeViews();
            loadStatistics();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing statistics", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        // Initialize back button
        btnBackArrow = findViewById(R.id.btnBackArrow);
        btnBackArrow.setOnClickListener(v -> finish());

        // Initialize charts
        ordersLineChart = findViewById(R.id.orders_line_chart);
        revenueLineChart = findViewById(R.id.revenue_line_chart);
        paymentPieChart = findViewById(R.id.payment_pie_chart);
        topDishesBarChart = findViewById(R.id.top_dishes_bar_chart);
        reviewRatingsBarChart = findViewById(R.id.review_ratings_bar_chart);
        userPrefsPieChart = findViewById(R.id.user_prefs_pie_chart);
        orderStatusPieChart = findViewById(R.id.order_status_pie_chart);

        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void loadStatistics() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    processStatisticsData(snapshot);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing statistics: " + e.getMessage(), e);
                    Toast.makeText(AdminStatisticsActivity.this,
                            "Error processing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(AdminStatisticsActivity.this,
                        "Failed to load statistics: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processStatisticsData(DataSnapshot snapshot) {
        Log.d(TAG, "Starting data processing...");

        // Data structures for analytics
        Map<String, Integer> ordersByDate = new HashMap<>();
        Map<String, Double> revenueByDate = new HashMap<>();
        Map<String, Integer> ordersByPayment = new HashMap<>();
        Map<String, Integer> dishOrderCount = new HashMap<>();
        Map<String, Integer> cuisinePrefs = new HashMap<>();
        Map<String, Float> restaurantRatings = new HashMap<>();
        Map<String, Integer> ordersByStatus = new HashMap<>();

        // Build dish-to-restaurant mapping FIRST
        Map<String, String> dishToRestaurantMap = new HashMap<>();
        Map<String, String> restaurantNames = new HashMap<>();

        Log.d(TAG, "Building dish-to-restaurant mapping...");

        try {
            // Build mapping from Dishes
            DataSnapshot dishesSnapshot = snapshot.child("Dishes");
            for (DataSnapshot restaurantSnap : dishesSnapshot.getChildren()) {
                String restaurantId = restaurantSnap.getKey();
                for (DataSnapshot dishSnap : restaurantSnap.getChildren()) {
                    String dishId = dishSnap.getKey();
                    dishToRestaurantMap.put(dishId, restaurantId);
                }
            }

            // Build restaurant info mapping
            DataSnapshot restaurantsSnapshot = snapshot.child("Restaurants");
            for (DataSnapshot restaurantSnap : restaurantsSnapshot.getChildren()) {
                String restaurantId = restaurantSnap.getKey();
                String name = restaurantSnap.child("name").getValue(String.class);
                Float rating = restaurantSnap.child("rating").getValue(Float.class);

                if (name != null) restaurantNames.put(restaurantId, name);
                if (name != null && rating != null) restaurantRatings.put(name, rating);
            }

            // Process orders
            DataSnapshot ordersSnapshot = snapshot.child("orders");
            int totalOrdersProcessed = 0;

            if (ordersSnapshot.exists()) {
                for (DataSnapshot userSnapshot : ordersSnapshot.getChildren()) {
                    for (DataSnapshot orderSnapshot : userSnapshot.getChildren()) {
                        try {
                            totalOrdersProcessed++;
                            processOrder(orderSnapshot, ordersByDate, revenueByDate, ordersByStatus,
                                    ordersByPayment, dishOrderCount);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing order: " + e.getMessage());
                        }
                    }
                }
            }

            Log.d(TAG, "Total orders processed: " + totalOrdersProcessed);

            // Process user preferences
            DataSnapshot prefsSnapshot = snapshot.child("UserPreferences");
            if (prefsSnapshot.exists()) {
                for (DataSnapshot pref : prefsSnapshot.getChildren()) {
                    try {
                        String cuisine = pref.child("cuisine").getValue(String.class);
                        if (cuisine != null) {
                            cuisinePrefs.put(cuisine, cuisinePrefs.getOrDefault(cuisine, 0) + 1);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing preference: " + e.getMessage());
                    }
                }
            }

            // Setup all charts
            setupOrdersChart(sortIntegerMapByDate(ordersByDate));
            setupRevenueChart(sortDoubleMapByDate(revenueByDate));
            setupPaymentChart(ordersByPayment);
            setupTopDishesChart(getTopEntries(dishOrderCount, 10));
            setupUserPrefsChart(cuisinePrefs);
            setupReviewRatingsChart(getTopEntries(restaurantRatings, 8));
            setupOrderStatusChart(ordersByStatus);

        } catch (Exception e) {
            Log.e(TAG, "Error in data processing: " + e.getMessage(), e);
            throw e;
        }
    }

    private void processOrder(DataSnapshot orderSnapshot, Map<String, Integer> ordersByDate,
                              Map<String, Double> revenueByDate, Map<String, Integer> ordersByStatus,
                              Map<String, Integer> ordersByPayment, Map<String, Integer> dishOrderCount) {

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

        if (itemsSnapshot.exists()) {
            for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                try {
                    String dishName = itemSnapshot.child("name").getValue(String.class);
                    Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                    if (quantity == null) quantity = 1;

                    // Track dish popularity
                    if (dishName != null) {
                        dishOrderCount.put(dishName, dishOrderCount.getOrDefault(dishName, 0) + quantity);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing item: " + e.getMessage());
                }
            }
        }
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
        try {
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
            ordersLineChart.getDescription().setEnabled(false);
            ordersLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            ordersLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            ordersLineChart.getXAxis().setGranularity(1f);
            ordersLineChart.getXAxis().setLabelRotationAngle(45f);
            ordersLineChart.getAxisLeft().setAxisMinimum(0f);
            ordersLineChart.getAxisRight().setEnabled(false);
            ordersLineChart.getLegend().setEnabled(false);
            ordersLineChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up orders chart: " + e.getMessage());
        }
    }

    private void setupRevenueChart(Map<String, Double> revenueByDate) {
        try {
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
            revenueLineChart.getDescription().setEnabled(false);
            revenueLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            revenueLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            revenueLineChart.getXAxis().setGranularity(1f);
            revenueLineChart.getXAxis().setLabelRotationAngle(45f);
            revenueLineChart.getAxisLeft().setAxisMinimum(0f);
            revenueLineChart.getAxisRight().setEnabled(false);
            revenueLineChart.getLegend().setEnabled(false);
            revenueLineChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up revenue chart: " + e.getMessage());
        }
    }

    private void setupPaymentChart(Map<String, Integer> ordersByPayment) {
        try {
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
            paymentPieChart.getDescription().setEnabled(false);
            paymentPieChart.setUsePercentValues(false);
            paymentPieChart.setEntryLabelColor(Color.BLACK);
            paymentPieChart.setEntryLabelTextSize(11f);
            paymentPieChart.getLegend().setTextSize(12f);
            paymentPieChart.setDrawHoleEnabled(true);
            paymentPieChart.setHoleRadius(40f);
            paymentPieChart.setTransparentCircleRadius(45f);
            paymentPieChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up payment chart: " + e.getMessage());
        }
    }

    private void setupTopDishesChart(Map<String, Integer> dishOrderCount) {
        try {
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
            topDishesBarChart.getDescription().setEnabled(false);
            topDishesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            topDishesBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            topDishesBarChart.getXAxis().setGranularity(1f);
            topDishesBarChart.getXAxis().setLabelRotationAngle(45f);
            topDishesBarChart.getAxisLeft().setAxisMinimum(0f);
            topDishesBarChart.getAxisRight().setEnabled(false);
            topDishesBarChart.getLegend().setEnabled(false);
            topDishesBarChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up top dishes chart: " + e.getMessage());
        }
    }

    private void setupUserPrefsChart(Map<String, Integer> cuisinePrefs) {
        try {
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
            userPrefsPieChart.getDescription().setEnabled(false);
            userPrefsPieChart.setUsePercentValues(false);
            userPrefsPieChart.setEntryLabelColor(Color.BLACK);
            userPrefsPieChart.setEntryLabelTextSize(11f);
            userPrefsPieChart.getLegend().setTextSize(12f);
            userPrefsPieChart.setDrawHoleEnabled(true);
            userPrefsPieChart.setHoleRadius(40f);
            userPrefsPieChart.setTransparentCircleRadius(45f);
            userPrefsPieChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up user prefs chart: " + e.getMessage());
        }
    }

    private void setupReviewRatingsChart(Map<String, Float> restaurantRatings) {
        try {
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
            reviewRatingsBarChart.getDescription().setEnabled(false);
            reviewRatingsBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            reviewRatingsBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            reviewRatingsBarChart.getXAxis().setGranularity(1f);
            reviewRatingsBarChart.getXAxis().setLabelRotationAngle(45f);
            reviewRatingsBarChart.getAxisLeft().setAxisMinimum(0f);
            reviewRatingsBarChart.getAxisLeft().setAxisMaximum(5f);
            reviewRatingsBarChart.getAxisRight().setEnabled(false);
            reviewRatingsBarChart.getLegend().setEnabled(false);
            reviewRatingsBarChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up review ratings chart: " + e.getMessage());
        }
    }

    private void setupOrderStatusChart(Map<String, Integer> ordersByStatus) {
        try {
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
            int[] colors = {
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#FF9800"),
                    Color.parseColor("#F44336"),
                    Color.parseColor("#2196F3")
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
            orderStatusPieChart.getDescription().setEnabled(false);
            orderStatusPieChart.setUsePercentValues(false);
            orderStatusPieChart.setEntryLabelColor(Color.BLACK);
            orderStatusPieChart.setEntryLabelTextSize(11f);
            orderStatusPieChart.getLegend().setTextSize(12f);
            orderStatusPieChart.setDrawHoleEnabled(true);
            orderStatusPieChart.setHoleRadius(40f);
            orderStatusPieChart.setTransparentCircleRadius(45f);
            orderStatusPieChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up order status chart: " + e.getMessage());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}