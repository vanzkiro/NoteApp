package com.example.noteapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "plans.db";
    private static final int DATABASE_VERSION = 13;
    private static final String TABLE_NAME = "plans";

    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_CONTENT = "content";
    private static final String COL_START_TIME = "start_time";
    private static final String COL_END_TIME = "end_time";
    private static final String COL_IS_COMPLETED = "is_completed";
    private static final String COL_REMINDER_HOUR = "reminder_hour";
    private static final String COL_REMINDER_MINUTE = "reminder_minute";
    private static final String COL_CATEGORY = "category";
    private static final String COL_PROGRESS = "progress";
    private static final String COL_IMAGE_PATH = "image_path"; // Cột mới

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_START_TIME + " INTEGER DEFAULT -1, " +
                COL_END_TIME + " INTEGER DEFAULT -1, " +
                COL_IS_COMPLETED + " INTEGER DEFAULT 0, " +
                COL_REMINDER_HOUR + " INTEGER DEFAULT -1, " +
                COL_REMINDER_MINUTE + " INTEGER DEFAULT -1, " +
                COL_CATEGORY + " TEXT DEFAULT 'Khác', " +
                COL_PROGRESS + " INTEGER DEFAULT 0, " +
                COL_IMAGE_PATH + " TEXT DEFAULT '')";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_REMINDER_HOUR + " INTEGER DEFAULT -1");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_REMINDER_MINUTE + " INTEGER DEFAULT -1");
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_CATEGORY + " TEXT DEFAULT 'Khác'");
        }
        if (oldVersion < 12) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_PROGRESS + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 13) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_IMAGE_PATH + " TEXT DEFAULT ''");
        }
    }

    public long addPlan(String title, String content, long startTime, long endTime, int reminderHour, int reminderMinute, String category, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_CONTENT, content);
        cv.put(COL_START_TIME, startTime);
        cv.put(COL_END_TIME, endTime);
        cv.put(COL_IS_COMPLETED, 0);
        cv.put(COL_REMINDER_HOUR, reminderHour);
        cv.put(COL_REMINDER_MINUTE, reminderMinute);
        cv.put(COL_CATEGORY, category);
        cv.put(COL_PROGRESS, calculateProgress(startTime, endTime, false));
        cv.put(COL_IMAGE_PATH, imagePath);
        return db.insert(TABLE_NAME, null, cv);
    }

    public void updatePlan(int id, String newTitle, String newContent, long startTime, long endTime, boolean isCompleted, int reminderHour, int reminderMinute, String category, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, newTitle);
        cv.put(COL_CONTENT, newContent);
        cv.put(COL_START_TIME, startTime);
        cv.put(COL_END_TIME, endTime);
        cv.put(COL_IS_COMPLETED, isCompleted ? 1 : 0);
        cv.put(COL_REMINDER_HOUR, reminderHour);
        cv.put(COL_REMINDER_MINUTE, reminderMinute);
        cv.put(COL_CATEGORY, category);
        cv.put(COL_IMAGE_PATH, imagePath);
        int progress = calculateProgress(startTime, endTime, isCompleted);
        cv.put(COL_PROGRESS, progress);
        if (progress == 100 && !isCompleted) {
            cv.put(COL_IS_COMPLETED, 1);
        }
        db.update(TABLE_NAME, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    private int calculateProgress(long startTime, long endTime, boolean isCompleted) {
        if (isCompleted) return 100;
        if (startTime == -1 || endTime == -1) return 0;
        long now = System.currentTimeMillis();
        if (now < startTime) return 0;
        if (now >= endTime) {
            updateCompletionStatus(startTime, endTime);
            return 100;
        }
        long totalDuration = endTime - startTime;
        long elapsed = now - startTime;
        return (int) ((elapsed * 100) / totalDuration);
    }

    private void updateCompletionStatus(long startTime, long endTime) {
        if (startTime == -1 || endTime == -1) return;
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_IS_COMPLETED, 1);
            cv.put(COL_PROGRESS, 100);
            db.update(TABLE_NAME, cv, COL_START_TIME + "=? AND " + COL_END_TIME + "=?",
                    new String[]{String.valueOf(startTime), String.valueOf(endTime)});
        }
    }

    public Plan getPlanById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_ID + "=?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            int iid = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
            long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_START_TIME));
            long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_END_TIME));
            int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_COMPLETED));
            int reminderHour = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_HOUR));
            int reminderMinute = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_MINUTE));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH));
            int progress = calculateProgress(startTime, endTime, isCompleted == 1);
            cursor.close();
            return new Plan(iid, title, content, startTime, endTime, isCompleted == 1 || progress == 100,
                    reminderHour, reminderMinute, category, progress, imagePath);
        }
        cursor.close();
        return null;
    }

    public ArrayList<Plan> getAllPlans() {
        ArrayList<Plan> plans = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_START_TIME));
                long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_END_TIME));
                int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_COMPLETED));
                int reminderHour = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_HOUR));
                int reminderMinute = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_MINUTE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH));
                int progress = calculateProgress(startTime, endTime, isCompleted == 1);
                plans.add(new Plan(id, title, content, startTime, endTime, isCompleted == 1 || progress == 100,
                        reminderHour, reminderMinute, category, progress, imagePath));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return plans;
    }

    public ArrayList<Plan> getPlansByCategory(String category) {
        ArrayList<Plan> plans = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        if (category.equals("Tất cả")) {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        } else {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_CATEGORY + "=?", new String[]{category});
        }

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_START_TIME));
                long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_END_TIME));
                int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_COMPLETED));
                int reminderHour = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_HOUR));
                int reminderMinute = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_MINUTE));
                String planCategory = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH));
                int progress = calculateProgress(startTime, endTime, isCompleted == 1);
                plans.add(new Plan(id, title, content, startTime, endTime, isCompleted == 1 || progress == 100,
                        reminderHour, reminderMinute, planCategory, progress, imagePath));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return plans;
    }

    public ArrayList<Plan> getIncompletePlans() {
        ArrayList<Plan> plans = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_IS_COMPLETED + "=0", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_START_TIME));
                long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_END_TIME));
                int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_COMPLETED));
                int reminderHour = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_HOUR));
                int reminderMinute = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_MINUTE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH));
                int progress = calculateProgress(startTime, endTime, isCompleted == 1);
                if (isCompleted == 0 && progress < 100) {
                    plans.add(new Plan(id, title, content, startTime, endTime, false,
                            reminderHour, reminderMinute, category, progress, imagePath));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return plans;
    }

    public ArrayList<Plan> getCompletedPlans() {
        ArrayList<Plan> plans = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_START_TIME));
                long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_END_TIME));
                int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_COMPLETED));
                int reminderHour = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_HOUR));
                int reminderMinute = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REMINDER_MINUTE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH));
                int progress = calculateProgress(startTime, endTime, isCompleted == 1);
                if (isCompleted == 1 || progress == 100) {
                    plans.add(new Plan(id, title, content, startTime, endTime, true,
                            reminderHour, reminderMinute, category, progress, imagePath));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return plans;
    }

    public void deletePlan(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearAllPlans() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }
}