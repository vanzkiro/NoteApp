package com.example.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    RecyclerView recyclerViewCompleted;
    TextView tvCompletedCount, tvCategoryStats;
    DatabaseHelper db;
    ArrayList<Plan> completedPlans;
    PlanAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo các view
        recyclerViewCompleted = findViewById(R.id.recyclerViewCompleted);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvCategoryStats = findViewById(R.id.tvCategoryStats);
        db = new DatabaseHelper(this);

        recyclerViewCompleted.setLayoutManager(new LinearLayoutManager(this));
        loadCompletedPlans();
    }

    private void loadCompletedPlans() {
        completedPlans = db.getCompletedPlans();
        Collections.sort(completedPlans, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        adapter = new PlanAdapter(completedPlans, plan -> {
            Intent intent = new Intent(StatisticsActivity.this, PlanDetailActivity.class);
            intent.putExtra("plan_id", plan.getId());
            intent.putExtra("read_only", true);
            startActivity(intent);
        });
        recyclerViewCompleted.setAdapter(adapter);

        // Cập nhật số lượng kế hoạch hoàn thành
        tvCompletedCount.setText("Số kế hoạch hoàn thành: " + completedPlans.size());

        // Thống kê theo danh mục
        HashMap<String, Integer> categoryCounts = new HashMap<>();
        for (Plan plan : completedPlans) {
            String category = plan.getCategory();
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        StringBuilder statsText = new StringBuilder("Thống kê theo danh mục:\n");
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            statsText.append(entry.getKey()).append(": ").append(entry.getValue()).append(" kế hoạch\n");
        }
        tvCategoryStats.setText(statsText.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompletedPlans();
    }
}
