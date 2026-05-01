package com.smartnotify.app.ui.adapter;

import android.content.ClipData;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;
import com.smartnotify.app.model.AppInfoModel;

import java.util.List;

public class UnassignedAppsAdapter extends RecyclerView.Adapter<UnassignedAppsAdapter.AppViewHolder> {

    private final List<AppInfoModel> appList;
    private final OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(AppInfoModel app);
    }

    public UnassignedAppsAdapter(List<AppInfoModel> appList, OnAppClickListener listener) {
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_priority_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfoModel app = appList.get(position);
        holder.imgIcon.setImageDrawable(app.getAppIcon());
        holder.tvName.setText(app.getAppName());

        // Normal click (Future use ke liye agar dialog dikhana ho)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppClick(app);
        });

        // 🔥 LONG PRESS → DRAG START (App ko utha kar rings me daalne ke liye)
        holder.itemView.setOnLongClickListener(v -> {
            // Drag hone par shadow create karna
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);

            // ClipData me hum app ka naam bhej sakte hain (optional)
            ClipData data = ClipData.newPlainText("app_name", app.getAppName());

            // Android 7.0+ ke liye startDragAndDrop use hota hai
            v.startDragAndDrop(
                    data,      // ClipData
                    shadow,    // Shadow builder
                    app,       // 🔥 localState (AppInfoModel object pass kar rahe hain ring ko)
                    0          // Flags
            );

            return true; // IMPORTANT: true matlab humne long click handle kar liya
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgAppIcon);
            tvName = itemView.findViewById(R.id.tvAppName);
        }
    }
}