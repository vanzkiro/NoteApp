package com.example.noteapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView tvPlanCount;
    SearchView searchView;
    Spinner spinnerCategoryFilter;
    DatabaseHelper db;
    ArrayList<Plan> planList;
    ArrayList<Plan> filteredPlanList;
    PlanAdapter adapter;
    private boolean titleAscending = true;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewPlans);
        tvPlanCount = findViewById(R.id.tvPlanCount);
        searchView = findViewById(R.id.searchView);
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter);
        db = new DatabaseHelper(this);

        updateCategorySpinner();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        restoreAlarms();
        loadPlans();

        MyFirebaseMessagingService.subscribeToTopic();
        subscribeToPlanCategories();

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPlanActivity.class);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPlans(newText, spinnerCategoryFilter.getSelectedItem().toString());
                return true;
            }
        });

        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                filterPlans(searchView.getQuery().toString(), selectedCategory);
                MyFirebaseMessagingService.subscribeToCategoryTopic(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterPlans(searchView.getQuery().toString(), "Tất cả");
            }
        });
    }

    private void updateCategorySpinner() {
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Tất cả");
        ArrayList<Plan> plans = db.getAllPlans();
        for (Plan plan : plans) {
            String category = plan.getCategory();
            if (!categories.contains(category) && !category.isEmpty()) {
                categories.add(category);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryFilter.setAdapter(adapter);
    }

    private void subscribeToPlanCategories() {
        ArrayList<Plan> plans = db.getAllPlans();
        ArrayList<String> categories = new ArrayList<>();
        for (Plan plan : plans) {
            String category = plan.getCategory();
            if (!categories.contains(category) && !category.isEmpty()) {
                categories.add(category);
                MyFirebaseMessagingService.subscribeToCategoryTopic(category);
            }
        }
        Log.d(TAG, "Subscribed to categories: " + categories);
    }

    private void filterPlans(String query, String category) {
        filteredPlanList = new ArrayList<>();
        planList = db.getIncompletePlans();

        if (category.equals("Tất cả")) {
            filteredPlanList.addAll(planList);
        } else {
            for (Plan plan : planList) {
                if (plan.getCategory().equals(category)) {
                    filteredPlanList.add(plan);
                }
            }
        }

        if (query != null && !query.trim().isEmpty()) {
            ArrayList<Plan> tempList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();
            for (Plan plan : filteredPlanList) {
                if (plan.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        plan.getContent().toLowerCase().contains(lowerCaseQuery)) {
                    tempList.add(plan);
                }
            }
            filteredPlanList = tempList;
        }
        sortPlans(filteredPlanList);
        adapter.updateData(filteredPlanList);
        tvPlanCount.setText("Số kế hoạch: " + filteredPlanList.size());
    }

    private void restoreAlarms() {
        Log.d(TAG, "Khôi phục báo thức cho tất cả kế hoạch");
        ArrayList<Plan> plans = db.getAllPlans();
        for (Plan plan : plans) {
            if (!plan.isCompleted() && plan.getProgress() < 100 &&
                    plan.getReminderHour() != -1 && plan.getReminderMinute() != -1) {
                Log.d(TAG, "Khôi phục báo thức cho kế hoạch: " + plan.getTitle());
                scheduleNextAlarm(this, plan);
                if (plan.getEndTime() != -1) {
                    scheduleCompletionAlarm(this, plan.getId(), plan.getEndTime());
                }
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNextAlarm(Context context, Plan plan) {
        if (plan.getReminderHour() == -1 || plan.getReminderMinute() == -1) return;
        if (plan.isCompleted() || plan.getProgress() == 100) return;

        long now = System.currentTimeMillis();
        int hour = plan.getReminderHour();
        int minute = plan.getReminderMinute();

        Calendar nextCal = Calendar.getInstance();
        nextCal.setTimeInMillis(now);
        nextCal.set(Calendar.HOUR_OF_DAY, hour);
        nextCal.set(Calendar.MINUTE, minute);
        nextCal.set(Calendar.SECOND, 0);
        nextCal.set(Calendar.MILLISECOND, 0);

        if (nextCal.getTimeInMillis() <= now) {
            nextCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (plan.getStartTime() != -1 && nextCal.getTimeInMillis() < plan.getStartTime()) {
            nextCal.setTimeInMillis(plan.getStartTime());
            nextCal.set(Calendar.HOUR_OF_DAY, hour);
            nextCal.set(Calendar.MINUTE, minute);
        }

        if (plan.getEndTime() != -1 && nextCal.getTimeInMillis() > plan.getEndTime()) {
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("plan_title", plan.getTitle());
        intent.putExtra("plan_id", plan.getId());
        PendingIntent pi = PendingIntent.getBroadcast(context, plan.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextCal.getTimeInMillis(), pi);
        Log.d(TAG, "Đã lên lịch báo thức cho kế hoạch: " + plan.getTitle() + " tại " + new Date(nextCal.getTimeInMillis()));
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleCompletionAlarm(Context context, int planId, long endTime) {
        if (endTime < System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CompletionReceiver.class);
        intent.putExtra("plan_id", planId);
        PendingIntent pi = PendingIntent.getBroadcast(context, planId + 1000000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pi);
        Log.d(TAG, "Đã lên lịch báo thức hoàn thành cho planId: " + planId + " tại " + new Date(endTime));
    }

    private void loadPlans() {
        planList = db.getIncompletePlans();
        filteredPlanList = new ArrayList<>(planList);
        Log.d(TAG, "Đã tải " + planList.size() + " kế hoạch chưa hoàn thành");
        for (Plan plan : planList) {
            Log.d(TAG, "Kế hoạch: " + plan.getTitle() + ", Hoàn thành: " + plan.isCompleted());
        }
        sortPlans(filteredPlanList);
        setupPlanAdapter(filteredPlanList);
        tvPlanCount.setText("Số kế hoạch: " + filteredPlanList.size());
        updateCategorySpinner();
    }

    private void setupPlanAdapter(ArrayList<Plan> plans) {
        adapter = new PlanAdapter(plans, plan -> {
            Intent intent = new Intent(MainActivity.this, PlanDetailActivity.class);
            intent.putExtra("plan_id", plan.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void sortPlans(ArrayList<Plan> list) {
        Collections.sort(list, (a, b) -> {
            if (titleAscending) {
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            } else {
                return b.getTitle().compareToIgnoreCase(a.getTitle());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear_all) {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa tất cả kế hoạch")
                    .setMessage("Bạn có chắc chắn muốn xóa tất cả kế hoạch?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        ArrayList<Plan> plans = db.getAllPlans();
                        ArrayList<String> categories = new ArrayList<>();
                        for (Plan plan : plans) {
                            String category = plan.getCategory();
                            if (!categories.contains(category) && !category.isEmpty()) {
                                categories.add(category);
                            }
                        }
                        for (String category : categories) {
                            MyFirebaseMessagingService.unsubscribeFromCategoryTopic(category);
                        }
                        db.clearAllPlans();
                        loadPlans();
                        Toast.makeText(this, "Đã xóa tất cả kế hoạch", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            return true;
        } else if (id == R.id.action_sort) {
            titleAscending = !titleAscending;
            loadPlans();
            Toast.makeText(this, "Sắp xếp tiêu đề " + (titleAscending ? "A-Z" : "Z-A"), Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_statistics) {
            startActivity(new Intent(this, StatisticsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle("Giới thiệu về ứng dụng")
                    .setMessage("Ứng dụng Ghi chú giúp bạn quản lý công việc hiệu quả!\n\n" +
                            "Cách hoạt động:\n" +
                            "- Quản lý kế hoạch\n" +
                            "- Thời gian và nhắc nhở: Đặt thời gian và " + "nhắc nhở hàng ngày tại giờ cố định.\n" +
                            "- Tiến độ: Tiến độ được tính bằng phần trăm thời gian đã trôi " +
                            "qua từ lúc bắt đầu đến kết thúc (ví dụ: 3 ngày/10 ngày = 30%).\n" +
                            "- Hoàn thành: Kế hoạch tự động đánh dấu hoàn thành khi hết thời gian hoặc đạt tiến độ 100%.\n" +
                            "- Thông báo Firebase: Nhận thông báo từ admin qua các danh mục đã chọn.\n\n" +
                            "Bắt đầu tổ chức công việc ngay hôm nay!")
                    .setPositiveButton("Đóng", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlans();
        subscribeToPlanCategories();
    }
}