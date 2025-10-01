package com.example.noteapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "plan_notifications";
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast with action: " + action);

        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Device rebooted, restoring alarms...");
            DatabaseHelper db = new DatabaseHelper(context);
            ArrayList<Plan> plans = db.getAllPlans();
            for (Plan plan : plans) {
                if (!plan.isCompleted() && plan.getProgress() < 100 &&
                        plan.getStartTime() != -1 && plan.getEndTime() != -1) {
                    Log.d(TAG, "Restoring alarm for plan: " + plan.getTitle());
                    scheduleNextAlarm(context, plan);
                    scheduleCompletionAlarm(context, plan.getId(), plan.getEndTime());
                }
            }
            return;
        }

        String title = intent.getStringExtra("plan_title");
        int planId = intent.getIntExtra("plan_id", -1);
        Log.d(TAG, "Showing notification for planId: " + planId + ", Title: " + title);

        if (planId == -1 || title == null) {
            Log.e(TAG, "Invalid planId or title in notification intent");
            return;
        }

        DatabaseHelper db = new DatabaseHelper(context);
        Plan plan = db.getPlanById(planId);
        if (plan == null || plan.isCompleted() || plan.getProgress() == 100) {
            Log.d(TAG, "Plan not found or already completed, cancelling notification");
            cancelNotification(context, planId);
            return;
        }

        // Tạo PendingIntent để mở PlanDetailActivity
        Intent detailIntent = new Intent(context, PlanDetailActivity.class);
        detailIntent.putExtra("plan_id", planId);
        detailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, planId, detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Nhắc nhở: " + title)
                .setContentText("Kiểm tra kế hoạch của bạn!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context);
        notificationManager.notify(planId, builder.build());
        Log.d(TAG, "Notification shown for planId: " + planId);

        // Lên lịch thông báo tiếp theo
        scheduleNextAlarm(context, plan);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Plan Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Kênh thông báo nhắc nhở kế hoạch");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNextAlarm(Context context, Plan plan) {
        if (plan.getStartTime() == -1 || plan.getEndTime() == -1 ||
                plan.isCompleted() || plan.getProgress() == 100 ||
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
        Log.d(TAG, "Scheduled next alarm for plan: " + plan.getTitle() + " at " + new Date(nextCal.getTimeInMillis()));
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
        Log.d(TAG, "Scheduled completion alarm for planId: " + planId + " at " + new Date(endTime));
    }

    public static void cancelNotification(Context context, int planId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, planId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        Log.d(TAG, "Cancelled notification for planId: " + planId);
    }
}