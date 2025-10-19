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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminStatisticsActivity extends AppCompatActivity {

    private LineChart ordersLineChart, revenueLineChart;
    private BarChart citiesBarChart;
    private PieChart paymentPieChart;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_statistics);

        getSupportActionBar().setTitle("Statistics & Analytics");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ordersLineChart = findViewById(R.id.orders_line_chart);
        revenueLineChart = findViewById(R.id.revenue_line_chart);
        citiesBarChart = findViewById(R.id.cities_bar_chart);
        paymentPieChart = findViewById(R.id.payment_pie_chart);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        loadStatistics();
    }

    private void loadStatistics() {
        databaseReference.child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> ordersByDate = new HashMap<>();
                Map<String, Double> revenueByDate = new HashMap<>();
                Map<String, Integer> ordersByCity = new HashMap<>();
                Map<String, Integer> ordersByPayment = new HashMap<>();

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot orderSnapshot : userSnapshot.getChildren()) {
                        String orderDate = orderSnapshot.child("orderDate").getValue(String.class);
                        Double totalAmount = orderSnapshot.child("totalAmount").getValue(Double.class);
                        String paymentMethod = orderSnapshot.child("paymentMethod").getValue(String.class);

                        // Extract date for grouping
                        if (orderDate != null) {
                            String dateKey = orderDate.substring(0, Math.min(10, orderDate.length()));

                            // Count orders by date
                            ordersByDate.put(dateKey, ordersByDate.getOrDefault(dateKey, 0) + 1);

                            // Sum revenue by date
                            if (totalAmount != null) {
                                revenueByDate.put(dateKey, revenueByDate.getOrDefault(dateKey, 0.0) + totalAmount);
                            }
                        }

                        // Count orders by payment method
                        if (paymentMethod != null) {
                            ordersByPayment.put(paymentMethod, ordersByPayment.getOrDefault(paymentMethod, 0) + 1);
                        }
                    }
                }

                // Get city data from users
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        for (DataSnapshot user : userSnapshot.getChildren()) {
                            String city = user.child("selectedCity").getValue(String.class);
                            if (city != null) {
                                ordersByCity.put(city, ordersByCity.getOrDefault(city, 0) + 1);
                            }
                        }

                        // Setup all charts
                        setupOrdersChart(ordersByDate);
                        setupRevenueChart(revenueByDate);
                        setupCitiesChart(ordersByCity);
                        setupPaymentChart(ordersByPayment);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminStatisticsActivity.this,
                        "Failed to load statistics", Toast.LENGTH_SHORT).show();
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

        BarDataSet dataSet = new BarDataSet(entries, "Orders by City");
        dataSet.setColor(Color.parseColor("#FF5722"));
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        citiesBarChart.setData(barData);
        citiesBarChart.getDescription().setText("Top Cities");
        citiesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        citiesBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        citiesBarChart.getXAxis().setGranularity(1f);
        citiesBarChart.invalidate();
    }

    private void setupPaymentChart(Map<String, Integer> ordersByPayment) {
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : ordersByPayment.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Payment Methods");

        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#673AB7"));
        colors.add(Color.parseColor("#4CAF50"));
        colors.add(Color.parseColor("#FF5722"));
        colors.add(Color.parseColor("#2196F3"));
        dataSet.setColors(colors);

        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        paymentPieChart.setData(pieData);
        paymentPieChart.getDescription().setText("Payment Distribution");
        paymentPieChart.setUsePercentValues(true);
        paymentPieChart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}