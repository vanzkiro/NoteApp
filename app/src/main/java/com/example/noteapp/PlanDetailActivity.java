package com.example.noteapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PlanDetailActivity extends AppCompatActivity {
    private TextView tvDetailTitle, tvDetailContent, tvDetailCategory, tvDetailReminder, tvDetailStatus, tvDetailProgress;
    private ImageView imageViewDetail;
    private Button btnEdit, btnDelete, btnShare;
    private ProgressBar progressBarDetail;
    private LinearLayout progressContainer;
    private DatabaseHelper db;
    private Plan plan;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo views
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailContent = findViewById(R.id.tvDetailContent);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailReminder = findViewById(R.id.tvDetailReminder);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailProgress = findViewById(R.id.tvDetailProgress);
        progressBarDetail = findViewById(R.id.progressBarDetail);
        progressContainer = findViewById(R.id.progressContainer);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnShare = findViewById(R.id.btnShare);
        imageViewDetail = findViewById(R.id.imageViewDetail);
        db = new DatabaseHelper(this);

        // Lấy planId từ Intent
        int planId = getIntent().getIntExtra("plan_id", -1);
        if (planId == -1) {
            finish();
            return;
        }

        // Lấy dữ liệu kế hoạch từ database
        plan = db.getPlanById(planId);
        if (plan == null) {
            finish();
            return;
        }

        // Hiển thị thông tin kế hoạch
        updatePlanDetails();

        // Xử lý sự kiện nút Sửa
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPlanActivity.class);
            intent.putExtra("plan_id", plan.getId());
            intent.putExtra("plan_title", plan.getTitle());
            intent.putExtra("plan_content", plan.getContent());
            intent.putExtra("plan_start", plan.getStartTime());
            intent.putExtra("plan_end", plan.getEndTime());
            intent.putExtra("plan_completed", plan.isCompleted());
            intent.putExtra("reminder_hour", plan.getReminderHour());
            intent.putExtra("reminder_minute", plan.getReminderMinute());
            intent.putExtra("plan_category", plan.getCategory());
            intent.putExtra("plan_image_path", plan.getImagePath());
            startActivity(intent);
        });

        // Xử lý sự kiện nút Xóa
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa kế hoạch")
                    .setMessage("Bạn có chắc chắn muốn xóa kế hoạch này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        String category = plan.getCategory();
                        if (plan.getImagePath() != null && !plan.getImagePath().isEmpty()) {
                            File imageFile = new File(plan.getImagePath());
                            if (imageFile.exists()) {
                                imageFile.delete();
                            }
                        }
                        NotificationReceiver.cancelNotification(this, plan.getId());
                        db.deletePlan(plan.getId());
                        ArrayList<Plan> plans = db.getPlansByCategory(category);
                        if (plans.isEmpty()) {
                            MyFirebaseMessagingService.unsubscribeFromCategoryTopic(category);
                        }
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Xử lý sự kiện nút Chia sẻ
        btnShare.setOnClickListener(v -> {
            String shareText = "Kế hoạch: " + plan.getTitle() + "\n" +
                    "Nội dung: " + plan.getContent() + "\n" +
                    "Danh mục: " + plan.getCategory();
            if (plan.getStartTime() != -1 && plan.getEndTime() != -1) {
                shareText += "\nThời gian: " + dateFormat.format(plan.getStartTime()) +
                        " đến " + dateFormat.format(plan.getEndTime()) +
                        "\nTiến độ: " + plan.getProgress() + "%" +
                        "\nTrạng thái: " + (plan.isCompleted() ? "Hoàn thành" : "Chưa hoàn thành");
            }
            if (plan.getReminderHour() != -1 && plan.getReminderMinute() != -1) {
                shareText += "\nNhắc nhở hàng ngày lúc: " +
                        String.format("%02d:%02d", plan.getReminderHour(), plan.getReminderMinute());
            }
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ kế hoạch"));
        });
    }

    private void updatePlanDetails() {
        tvDetailTitle.setText(plan.getTitle());
        tvDetailContent.setText(plan.getContent());
        tvDetailCategory.setText("Danh mục: " + plan.getCategory());

        // Hiển thị hoặc ẩn trạng thái và tiến độ
        if (plan.getStartTime() != -1 && plan.getEndTime() != -1) {
            tvDetailStatus.setVisibility(View.VISIBLE);
            progressContainer.setVisibility(View.VISIBLE);
            tvDetailStatus.setText(plan.isCompleted() ? "Hoàn thành" : "Chưa hoàn thành");
            tvDetailStatus.setTextColor(plan.isCompleted() ? getResources().getColor(android.R.color.holo_green_dark) :
                    getResources().getColor(android.R.color.holo_red_dark));
            progressBarDetail.setProgress(plan.getProgress());
            tvDetailProgress.setText("Tiến độ: " + plan.getProgress() + "%");
        } else {
            tvDetailStatus.setVisibility(View.GONE);
            progressContainer.setVisibility(View.GONE);
        }

        // Hiển thị thông tin nhắc nhở và thời gian
        updateReminderDisplay();

        // Hiển thị hình ảnh nếu có
        if (plan.getImagePath() != null && !plan.getImagePath().isEmpty()) {
            File imageFile = new File(plan.getImagePath());
            if (imageFile.exists()) {
                imageViewDetail.setImageURI(Uri.fromFile(imageFile));
                imageViewDetail.setVisibility(View.VISIBLE);
            } else {
                imageViewDetail.setVisibility(View.GONE);
            }
        } else {
            imageViewDetail.setVisibility(View.GONE);
        }
    }

    private void updateReminderDisplay() {
        String text = "";
        if (plan.getStartTime() != -1 && plan.getEndTime() != -1) {
            Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(plan.getStartTime());
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(plan.getEndTime());
            text = "Kế hoạch từ: " + dateFormat.format(startCal.getTime()) +
                    " đến: " + dateFormat.format(endCal.getTime());
        }
        if (plan.getReminderHour() != -1 && plan.getReminderMinute() != -1) {
            text += (text.isEmpty() ? "" : ", ") +
                    "Nhắc nhở hàng ngày lúc: " +
                    String.format("%02d:%02d", plan.getReminderHour(), plan.getReminderMinute());
        }
        if (text.isEmpty()) {
            text = "Chưa đặt thời gian kế hoạch";
        }
        tvDetailReminder.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật lại dữ liệu khi quay lại từ chỉnh sửa
        int planId = getIntent().getIntExtra("plan_id", -1);
        if (planId != -1) {
            plan = db.getPlanById(planId);
            if (plan == null) {
                finish();
                return;
            }
            updatePlanDetails();
        }
    }
}