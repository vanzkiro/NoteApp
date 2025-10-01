package com.example.noteapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class AddPlanActivity extends AppCompatActivity {

    AutoCompleteTextView edtCategory;
    EditText edtTitle, edtContent;
    Button btnSave;
    ImageButton btnSetReminder, btnSetReminderTime, btnCancelReminder;
    TextView tvReminderPeriod;
    DatabaseHelper db;
    int planId = -1;
    long startTime = -1;
    long endTime = -1;
    int reminderHour = -1;
    int reminderMinute = -1;
    String category = "Khác";
    boolean isCompleted = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final int REQUEST_CODE_NOTIFICATION = 100;
    private static final int REQUEST_CODE_BATTERY = 101;
    private static final int REQUEST_CODE_ALARM = 102;
    private static final int REQUEST_CODE_PICK_IMAGE = 103;
    private ImageView imageViewPlan;
    private Button btnPickImage, btnRemoveImage;
    private String imagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plan);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo các view
        edtTitle = findViewById(R.id.edtTitle);
        edtContent = findViewById(R.id.edtContent);
        edtCategory = findViewById(R.id.edtCategory);
        btnSave = findViewById(R.id.btnSave);
        btnSetReminder = findViewById(R.id.btnSetReminder);
        btnSetReminderTime = findViewById(R.id.btnSetReminderTime);
        btnCancelReminder = findViewById(R.id.btnCancelReminder);
        tvReminderPeriod = findViewById(R.id.tvReminderPeriod);
        imageViewPlan = findViewById(R.id.imageViewPlan);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        db = new DatabaseHelper(this);

        // Thiết lập gợi ý danh mục
        setupCategorySuggestions();

        // Kiểm tra quyền
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_PICK_IMAGE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PICK_IMAGE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, REQUEST_CODE_ALARM);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_BATTERY);
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(new NotificationReceiver(), filter);

        // Tải dữ liệu kế hoạch nếu đang chỉnh sửa
        if (getIntent() != null) {
            planId = getIntent().getIntExtra("plan_id", -1);
            String title = getIntent().getStringExtra("plan_title");
            String content = getIntent().getStringExtra("plan_content");
            startTime = getIntent().getLongExtra("plan_start", -1);
            endTime = getIntent().getLongExtra("plan_end", -1);
            isCompleted = getIntent().getBooleanExtra("plan_completed", false);
            reminderHour = getIntent().getIntExtra("reminder_hour", -1);
            reminderMinute = getIntent().getIntExtra("reminder_minute", -1);
            category = getIntent().getStringExtra("plan_category");
            imagePath = getIntent().getStringExtra("plan_image_path");

            if (planId != -1) {
                edtTitle.setText(title);
                edtContent.setText(content);
                edtCategory.setText(category);
                btnSave.setText("Cập nhật");
                updateReminderDisplay();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        imageViewPlan.setImageURI(Uri.fromFile(imageFile));
                        imageViewPlan.setVisibility(View.VISIBLE);
                        btnRemoveImage.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        // Chọn hình ảnh
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });

        // Xóa hình ảnh
        btnRemoveImage.setOnClickListener(v -> {
            if (!imagePath.isEmpty()) {
                File oldImageFile = new File(imagePath);
                if (oldImageFile.exists()) {
                    oldImageFile.delete(); // Xóa tệp hình ảnh cũ
                }
            }
            imagePath = "";
            imageViewPlan.setImageURI(null);
            imageViewPlan.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
            Toast.makeText(this, "Đã xóa hình ảnh", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String content = edtContent.getText().toString().trim();
            String selectedCategory = edtCategory.getText().toString().trim();

            if (selectedCategory.isEmpty()) {
                selectedCategory = "Khác";
            }

            if (!title.isEmpty() && !content.isEmpty()) {
                if (planId == -1) {
                    long newIdLong = db.addPlan(title, content, startTime, endTime, reminderHour, reminderMinute, selectedCategory, imagePath);
                    int newPlanId = (int) newIdLong;
                    MyFirebaseMessagingService.subscribeToCategoryTopic(selectedCategory);
                    Plan newPlan = db.getPlanById(newPlanId);
                    if (!newPlan.isCompleted() && newPlan.getProgress() < 100 &&
                            startTime != -1 && endTime != -1 && reminderHour != -1 && reminderMinute != -1) {
                        scheduleNextAlarm(this, newPlan);
                        scheduleCompletionAlarm(this, newPlanId, endTime);
                    }
                    Toast.makeText(this, "Đã thêm kế hoạch", Toast.LENGTH_SHORT).show();
                } else {
                    String oldCategory = category;
                    db.updatePlan(planId, title, content, startTime, endTime, isCompleted, reminderHour, reminderMinute, selectedCategory, imagePath);
                    if (!selectedCategory.equals(oldCategory)) {
                        MyFirebaseMessagingService.subscribeToCategoryTopic(selectedCategory);
                        ArrayList<Plan> plans = db.getPlansByCategory(oldCategory);
                        if (plans.isEmpty()) {
                            MyFirebaseMessagingService.unsubscribeFromCategoryTopic(oldCategory);
                        }
                    }
                    cancelPlanAlarms(planId);
                    Plan updatedPlan = db.getPlanById(planId);
                    if (!updatedPlan.isCompleted() && updatedPlan.getProgress() < 100 &&
                            startTime != -1 && endTime != -1 && reminderHour != -1 && reminderMinute != -1) {
                        scheduleNextAlarm(this, updatedPlan);
                        scheduleCompletionAlarm(this, planId, endTime);
                    }
                    Toast.makeText(this, "Đã cập nhật kế hoạch", Toast.LENGTH_SHORT).show();
                }
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Nhập tiêu đề và nội dung!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetReminder.setOnClickListener(v -> showDateRangePicker());

        btnSetReminderTime.setOnClickListener(v -> {
            if (startTime == -1) {
                Toast.makeText(this, "Vui lòng đặt khoảng thời gian trước!", Toast.LENGTH_SHORT).show();
                return;
            }
            showTimePicker();
        });

        btnCancelReminder.setOnClickListener(v -> {
            if (startTime != -1 || endTime != -1 || reminderHour != -1 || reminderMinute != -1) {
                startTime = -1;
                endTime = -1;
                reminderHour = -1;
                reminderMinute = -1;
                tvReminderPeriod.setText("Chưa đặt thời gian kế hoạch");
                if (planId != -1) {
                    cancelPlanAlarms(planId);
                }
                Toast.makeText(this, "Đã hủy thời gian kế hoạch", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Chưa có thời gian kế hoạch để hủy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCategorySuggestions() {
        // Lấy danh sách danh mục từ cơ sở dữ liệu
        ArrayList<Plan> plans = db.getAllPlans();
        HashSet<String> categories = new HashSet<>();
        for (Plan plan : plans) {
            String category = plan.getCategory();
            if (!category.isEmpty() && !category.equals("Khác")) {
                categories.add(category);
            }
        }
        // Thêm danh mục mặc định
        categories.add("Khác");

        // Thiết lập adapter cho AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(categories));
        edtCategory.setAdapter(adapter);
    }

    private void updateReminderDisplay() {
        if (startTime != -1 && endTime != -1) {
            Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(startTime);
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(endTime);
            String text = "Kế hoạch từ: " + dateFormat.format(startCal.getTime()) +
                    " đến: " + dateFormat.format(endCal.getTime());
            if (reminderHour != -1 && reminderMinute != -1) {
                text += ", Nhắc nhở: " + String.format("%02d:%02d", reminderHour, reminderMinute);
            } else {
                text += ", Chưa đặt nhắc nhở";
            }
            tvReminderPeriod.setText(text);
        } else {
            tvReminderPeriod.setText("Chưa đặt thời gian kế hoạch");
        }
    }

    private void showDateRangePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog startDatePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar startDateTime = Calendar.getInstance();
            startDateTime.set(year, month, dayOfMonth, 0, 0, 0);
            startTime = startDateTime.getTimeInMillis();

            DatePickerDialog endDatePicker = new DatePickerDialog(this, (view1, endYear, endMonth, endDayOfMonth) -> {
                Calendar endDateTime = Calendar.getInstance();
                endDateTime.set(endYear, endMonth, endDayOfMonth, 23, 59, 59);
                endTime = endDateTime.getTimeInMillis();

                if (endTime >= startTime) {
                    updateReminderDisplay();
                } else {
                    Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                    startTime = -1;
                    endTime = -1;
                    reminderHour = -1;
                    reminderMinute = -1;
                    tvReminderPeriod.setText("Chưa đặt thời gian kế hoạch");
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            endDatePicker.getDatePicker().setMinDate(startDateTime.getTimeInMillis());
            endDatePicker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        startDatePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        startDatePicker.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            reminderHour = hourOfDay;
            reminderMinute = minute;
            updateReminderDisplay();
        }, (reminderHour != -1 ? reminderHour : 12), (reminderMinute != -1 ? reminderMinute : 0), true);
        timePickerDialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNextAlarm(Context context, Plan plan) {
        if (plan.getStartTime() == -1 || plan.getEndTime() == -1 || plan.isCompleted() || plan.getProgress() == 100 ||
                plan.getReminderHour() == -1 || plan.getReminderMinute() == -1) return;
        long now = System.currentTimeMillis();
        if (plan.getEndTime() < now) return;

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(plan.getStartTime());
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

        long startMillis = startCal.getTimeInMillis();
        if (nextCal.getTimeInMillis() < startMillis) {
            nextCal.setTimeInMillis(startMillis);
        }

        if (nextCal.getTimeInMillis() > plan.getEndTime()) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("plan_title", plan.getTitle());
        intent.putExtra("plan_id", plan.getId());
        PendingIntent pi = PendingIntent.getBroadcast(context, plan.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextCal.getTimeInMillis(), pi);
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
    }

    private void cancelPlanAlarms(int planId) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent nIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent nPi = PendingIntent.getBroadcast(this, planId, nIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(nPi);

        Intent cIntent = new Intent(this, CompletionReceiver.class);
        PendingIntent cPi = PendingIntent.getBroadcast(this, planId + 1000000, cIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(cPi);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Vui lòng cấp quyền thông báo để nhận nhắc nhở!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Vui lòng cấp quyền truy cập hình ảnh!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                // Sao chép hình ảnh vào bộ nhớ nội bộ
                imagePath = copyImageToInternalStorage(selectedImage);
                File imageFile = new File(imagePath);
                imageViewPlan.setImageURI(Uri.fromFile(imageFile));
                imageViewPlan.setVisibility(View.VISIBLE);
                btnRemoveImage.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Toast.makeText(this, "Lỗi khi sao chép hình ảnh!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (requestCode == REQUEST_CODE_BATTERY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Toast.makeText(this, "Vui lòng bỏ qua tối ưu hóa pin để đảm bảo thông báo hoạt động!", Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == REQUEST_CODE_ALARM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Vui lòng cấp quyền báo thức chính xác để nhận thông báo!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private String copyImageToInternalStorage(Uri uri) throws IOException {
        // Tạo thư mục lưu trữ trong bộ nhớ nội bộ
        File directory = new File(getFilesDir(), "images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Tạo tên tệp duy nhất
        String fileName = "plan_image_" + System.currentTimeMillis() + ".jpg";
        File file = new File(directory, fileName);

        // Sao chép dữ liệu từ Uri vào tệp
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return file.getAbsolutePath();
    }
}