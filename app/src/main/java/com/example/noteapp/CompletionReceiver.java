package com.example.noteapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class CompletionReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "plan_reminder_channel";
    private static final String TAG = "CompletionReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        int planId = intent.getIntExtra("plan_id", -1);
        if (planId != -1) {
            DatabaseHelper db = new DatabaseHelper(context);
            Plan plan = db.getPlanById(planId);
            if (plan != null) {
                Log.d(TAG, "Đánh dấu kế hoạch hoàn thành: " + plan.getTitle());
                String category = plan.getCategory();
                // Thêm imagePath vào tham số
                db.updatePlan(planId, plan.getTitle(), plan.getContent(), plan.getStartTime(),
                        plan.getEndTime(), true, plan.getReminderHour(),
                        plan.getReminderMinute(), category, plan.getImagePath());
                cancelPlanAlarms(context, planId);
                // Kiểm tra nếu không còn kế hoạch nào trong danh mục
                ArrayList<Plan> plans = db.getPlansByCategory(category);
                if (plans.isEmpty()) {
                    MyFirebaseMessagingService.unsubscribeFromCategoryTopic(category);
                }

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Plan Reminders",
                            NotificationManager.IMPORTANCE_HIGH);
                    channel.enableVibration(true);
                    channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    notificationManager.createNotificationChannel(channel);
                }

                Intent detailIntent = new Intent(context, PlanDetailActivity.class);
                detailIntent.putExtra("plan_id", planId);
                detailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, planId, detailIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Kế hoạch hoàn thành: " + plan.getTitle())
                        .setContentText("Kế hoạch của bạn đã hoàn thành!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(pendingIntent);

                notificationManager.notify(planId + 1000000, builder.build());
                Log.d(TAG, "Sent completion notification for plan: " + plan.getTitle());
            } else {
                Log.e(TAG, "Plan not found for ID: " + planId);
            }
        } else {
            Log.e(TAG, "Invalid planId: " + planId);
        }
    }

    private void cancelPlanAlarms(Context context, int planId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent nIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent nPi = PendingIntent.getBroadcast(context, planId, nIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(nPi);

        Intent cIntent = new Intent(context, CompletionReceiver.class);
        PendingIntent cPi = PendingIntent.getBroadcast(context, planId + 1000000, cIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(cPi);
    }
}