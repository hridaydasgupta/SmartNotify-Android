package com.smartnotify.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;
import com.smartnotify.app.model.AppInfoModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddAppsAdapter extends RecyclerView.Adapter<AddAppsAdapter.ViewHolder> {

    private final List<AppInfoModel> appList;
    private final Set<String> selectedApps = new HashSet<>();
    private final OnSelectionChangeListener selectionChangeListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }

    public AddAppsAdapter(List<AppInfoModel> appList, OnSelectionChangeListener listener) {
        this.appList = appList;
        this.selectionChangeListener = listener;
    }

    // Konsi apps select hui hain unka package name return karega
    public Set<String> getSelectedApps() {
        return selectedApps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfoModel app = appList.get(position);

        holder.imgAppIcon.setImageDrawable(app.getAppIcon());
        holder.tvAppName.setText(app.getAppName());

        // Pehle se select hai ya nahi check karo
        holder.cbSelectApp.setChecked(selectedApps.contains(app.getPackageName()));

        // Poori row pe click hone par checkbox toggle karo
        holder.itemView.setOnClickListener(v -> {
            if (selectedApps.contains(app.getPackageName())) {
                selectedApps.remove(app.getPackageName()); // Uncheck
                holder.cbSelectApp.setChecked(false);
            } else {
                selectedApps.add(app.getPackageName()); // Check
                holder.cbSelectApp.setChecked(true);
            }

            // UI Button (Add (2)) ko update karne ke liye listener call karo
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(selectedApps.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAppIcon;
        TextView tvAppName;
        CheckBox cbSelectApp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAppIcon = itemView.findViewById(R.id.imgAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            cbSelectApp = itemView.findViewById(R.id.cbSelectApp);
        }
    }
}