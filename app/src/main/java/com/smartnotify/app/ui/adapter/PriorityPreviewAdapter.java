package com.smartnotify.app.ui.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;

import java.util.List;

public class PriorityPreviewAdapter extends RecyclerView.Adapter<PriorityPreviewAdapter.ViewHolder> {

    private List<String> appList; // Isko non-final rakha hai taaki list update ho sake
    private final PackageManager packageManager;
    private final OnRemoveClickListener removeClickListener;

    // 🔥 Edit Mode track karne ke liye
    private boolean isEditMode = false;

    public interface OnRemoveClickListener {
        void onRemoveClick(String packageName, int position);
    }

    public PriorityPreviewAdapter(Context context, List<String> appList, OnRemoveClickListener listener) {
        this.appList = appList;
        this.packageManager = context.getPackageManager();
        this.removeClickListener = listener;
    }

    // 🔥 Edit Mode on/off karne ke liye
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged(); // List ko refresh karega taaki ❌ icons dikh/chhup jayein
    }

    // 🔥 Safety method: Agar future mein puri list ek sath refresh karni ho
    public void updateList(List<String> newList) {
        this.appList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tumhara XML layout file
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_priority_app_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String packageName = appList.get(position);

        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            holder.imgAppIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo));
            holder.tvAppName.setText(packageManager.getApplicationLabel(appInfo));
        } catch (PackageManager.NameNotFoundException e) {
            holder.imgAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            holder.tvAppName.setText("Unknown");
        }

        // 🔥 Logic: Agar edit mode ON hai tabhi ❌ dikhao, warna hide kardo (GONE)
        if (isEditMode) {
            holder.imgRemove.setVisibility(View.VISIBLE);
        } else {
            holder.imgRemove.setVisibility(View.GONE);
        }

        // 🔥 CRASH FIX: Fast click check 🔥
        holder.imgRemove.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();

            // Check karo ki position valid hai aur -1 (NO_POSITION) nahi hai
            if (currentPosition != RecyclerView.NO_POSITION && removeClickListener != null) {
                // Ab purane packageName ki jagah hum current, updated package name bhejenge
                String currentPackage = appList.get(currentPosition);
                removeClickListener.onRemoveClick(currentPackage, currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList != null ? appList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAppIcon;
        TextView tvAppName;
        ImageView imgRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAppIcon = itemView.findViewById(R.id.imgAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            imgRemove = itemView.findViewById(R.id.imgRemove);
        }
    }
}