package com.example.noteapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    private ArrayList<com.example.noteapp.Plan> plans;
    private final OnPlanClickListener planClickListener;

    public interface OnPlanClickListener {
        void onPlanClick(com.example.noteapp.Plan plan);
    }

    public PlanAdapter(ArrayList<com.example.noteapp.Plan> plans, OnPlanClickListener planClickListener) {
        this.plans = plans;
        this.planClickListener = planClickListener;
    }

    public void updateData(ArrayList<com.example.noteapp.Plan> newPlans) {
        this.plans = newPlans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        com.example.noteapp.Plan plan = plans.get(position);
        holder.tvTitle.setText(plan.getTitle());
        holder.tvCategory.setText("Danh mục: " + plan.getCategory());

        // Kiểm tra xem kế hoạch có đặt lịch không
        if (plan.getStartTime() != -1 && plan.getEndTime() != -1) {
            holder.tvStatus.setText(plan.isCompleted() || plan.getProgress() == 100 ? "Hoàn thành" : "Chưa hoàn thành");
            holder.tvProgress.setText("Tiến độ: " + plan.getProgress() + "%");
            holder.progressBarPlan.setProgress(plan.getProgress());
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvProgress.setVisibility(View.VISIBLE);
            holder.progressBarPlan.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
            holder.tvProgress.setVisibility(View.GONE);
            holder.progressBarPlan.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> planClickListener.onPlanClick(plan));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvCategory, tvProgress;
        ProgressBar progressBarPlan;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvStatus = itemView.findViewById(R.id.tvPlanStatus);
            tvCategory = itemView.findViewById(R.id.tvPlanCategory);
            tvProgress = itemView.findViewById(R.id.tvPlanProgress);
            progressBarPlan = itemView.findViewById(R.id.progressBarPlan);
        }
    }
}