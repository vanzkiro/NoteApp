package com.example.noteapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PlanDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvStatus, tvReminder, tvCategory, tvProgress;
    private ProgressBar progressBarDetail;
    private ImageView imageViewDetail;
    private Button btnEdit, btnDelete, btnShare;
    private DatabaseHelper db;
    private Plan plan;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo các view
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvContent = findViewById(R.id.tvDetailContent);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvReminder = findViewById(R.id.tvDetailReminder);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvProgress = findViewById(R.id.tvDetailProgress);
        progressBarDetail = findViewById(R.id.progressBarDetail);
        imageViewDetail = findViewById(R.id.imageViewDetail);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnShare = findViewById(R.id.btnShare);
        db = new DatabaseHelper(this);

        Intent intent = getIntent();
        int planId = intent.getIntExtra("plan_id", -1);
        plan = db.getPlanById(planId);

        if (plan == null) {
            Toast.makeText(this, "Không tìm thấy kế hoạch!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateUI();

        boolean isReadOnly = intent.getBooleanExtra("read_only", false);
        if (isReadOnly) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnShare.setVisibility(View.GONE);
        } else {
            btnEdit.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, AddPlanActivity.class);
                editIntent.putExtra("plan_id", plan.getId());
                editIntent.putExtra("plan_title", plan.getTitle());
                editIntent.putExtra("plan_content", plan.getContent());
                editIntent.putExtra("plan_start", plan.getStartTime());
                editIntent.putExtra("plan_end", plan.getEndTime());
                editIntent.putExtra("plan_completed", plan.isCompleted() || plan.getProgress() == 100);
                editIntent.putExtra("reminder_hour", plan.getReminderHour());
                editIntent.putExtra("reminder_minute", plan.getReminderMinute());
                editIntent.putExtra("plan_category", plan.getCategory());
                editIntent.putExtra("plan_image_path", plan.getImagePath());
                startActivity(editIntent);
            });

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa kế hoạch")
                        .setMessage("Bạn có chắc chắn muốn xóa kế hoạch \"" + plan.getTitle() + "\"?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            String category = plan.getCategory();
                            if (plan.getImagePath() != null && !plan.getImagePath().isEmpty()) {
                                File imageFile = new File(plan.getImagePath());
                                if (imageFile.exists()) {
                                    imageFile.delete(); // Xóa tệp hình ảnh khi xóa kế hoạch
                                }
                            }
                            db.deletePlan(plan.getId());
                            ArrayList<Plan> plans = db.getPlansByCategory(category);
                            if (plans.isEmpty()) {
                                MyFirebaseMessagingService.unsubscribeFromCategoryTopic(category);
                            }
                            Toast.makeText(this, "Đã xóa kế hoạch \"" + plan.getTitle() + "\"", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

            btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, plan.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        plan.getTitle() + "\n\n" +
                                plan.getContent() + "\nDanh mục: " + plan.getCategory() +
                                (plan.getStartTime() != -1 && plan.getEndTime() != -1 ?
                                        "\nTiến độ: " + plan.getProgress() + "%" +
                                                (plan.isCompleted() || plan.getProgress() == 100 ?
                                                        "\nTrạng thái: Hoàn thành" : "\nTrạng thái: Chưa hoàn thành") : "") +
                                buildReminderText(plan));
                startActivity(Intent.createChooser(shareIntent, "Chia sẻ kế hoạch qua"));
            });
        }
    }

    private void updateUI() {
        tvTitle.setText(plan.getTitle());
        tvContent.setText(plan.getContent());
        tvCategory.setText("Danh mục: " + plan.getCategory());
        tvReminder.setText(buildReminderText(plan));
        if (plan.getImagePath() != null && !plan.getImagePath().isEmpty()) {
            try {
                File imageFile = new File(plan.getImagePath());
                if (imageFile.exists()) {
                    imageViewDetail.setImageURI(Uri.fromFile(imageFile));
                    imageViewDetail.setVisibility(View.VISIBLE);
                } else {
                    imageViewDetail.setVisibility(View.GONE);
                    Toast.makeText(this, "Hình ảnh không tồn tại!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                imageViewDetail.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi khi tải hình ảnh!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            imageViewDetail.setVisibility(View.GONE);
        }
        if (plan.getStartTime() != -1 && plan.getEndTime() != -1) {
            tvStatus.setText(plan.isCompleted() || plan.getProgress() == 100 ? "Hoàn thành" : "Chưa hoàn thành");
            tvProgress.setText("Tiến độ: " + plan.getProgress() + "%");
            progressBarDetail.setProgress(plan.getProgress());
            tvStatus.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.VISIBLE);
            progressBarDetail.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);
            progressBarDetail.setVisibility(View.GONE);
        }
    }

    private String buildReminderText(Plan plan) {
        if (plan.getStartTime() == -1) {
            return "";
        }
        String text = "\nKế hoạch từ: " + sdf.format(new Date(plan.getStartTime()));
        if (plan.getEndTime() != -1) {
            text += " đến: " + sdf.format(new Date(plan.getEndTime()));
        }
        if (plan.getReminderHour() != -1 && plan.getReminderMinute() != -1) {
            text += ", Nhắc nhở: " + String.format("%02d:%02d", plan.getReminderHour(), plan.getReminderMinute());
        }
        return text;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (plan != null) {
            plan = db.getPlanById(plan.getId());
            if (plan != null) {
                updateUI();
            } else {
                Toast.makeText(this, "Kế hoạch không còn tồn tại!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}