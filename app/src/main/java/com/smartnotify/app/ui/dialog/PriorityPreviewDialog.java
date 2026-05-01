package com.smartnotify.app.ui.dialog;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;
import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.ui.adapter.PriorityPreviewAdapter;
import com.smartnotify.app.ui.setup.PrioritySetupActivity;
import com.smartnotify.app.util.PriorityConstants;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PriorityPreviewDialog extends DialogFragment {

    private static final String ARG_PRIORITY = "arg_priority";
    private int priority;
    private NotificationRepository repository;
    private PriorityPreviewAdapter adapter;
    private boolean isEditing = false;

    private ExecutorService executor;
    private Handler handler;
    private RecyclerView rvApps;

    public static void show(FragmentManager fm, int priority) {
        PriorityPreviewDialog dialog = new PriorityPreviewDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_PRIORITY, priority);
        dialog.setArguments(args);
        dialog.show(fm, "PriorityPreviewDialog");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔥 Is theme se humein full screen translucent background milta hai
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
        repository = new NotificationRepository(requireContext());
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            priority = getArguments().getInt(ARG_PRIORITY);
        }

        View view = inflater.inflate(R.layout.dialog_priority_preview, container, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvEdit = view.findViewById(R.id.tvEdit);
        ImageView btnAddApps = view.findViewById(R.id.btnAddApps);
        rvApps = view.findViewById(R.id.rvApps);

        // Priority Titles
        if (priority == PriorityConstants.HIGH) {
            tvTitle.setText("High Priority");
        } else if (priority == PriorityConstants.MEDIUM) {
            tvTitle.setText("Medium Priority");
        } else {
            tvTitle.setText("Low Priority");
        }

        // Folder structure ke liye 4 columns (ya tumhare choice ke hisaab se 3)
        rvApps.setLayoutManager(new GridLayoutManager(getContext(), 4));

        loadAndSetApps();

        // 🔥 EDIT MODE TOGGLE 🔥
        tvEdit.setOnClickListener(v -> {
            isEditing = !isEditing;
            tvEdit.setText(isEditing ? "Done" : "Edit");
            btnAddApps.setVisibility(isEditing ? View.GONE : View.VISIBLE);

            if (adapter != null) {
                adapter.setEditMode(isEditing);
            }
        });

        btnAddApps.setOnClickListener(v -> {
            AddAppsDialog dialog = AddAppsDialog.newInstance(priority);
            dialog.setListener(this::loadAndSetApps);
            dialog.show(getChildFragmentManager(), "AddAppsDialog");
        });

        // Background par click karne se dismiss ho jaye
        view.setOnClickListener(v -> dismiss());

        return view;
    }

    private void loadAndSetApps() {
        executor.execute(() -> {
            List<String> apps = repository.getAppsByPriority(priority);

            handler.post(() -> {
                adapter = new PriorityPreviewAdapter(requireContext(), apps, (packageName, position) -> {
                    if (position >= 0 && position < apps.size()) {
                        // 1. Update DB
                        repository.saveAppPriority(packageName, PriorityConstants.UNASSIGNED);

                        // 2. Refresh Background Service Cache
                        Intent updateIntent = new Intent("com.smartnotify.CACHE_UPDATE");
                        requireContext().sendBroadcast(updateIntent);

                        // 3. UI Updates
                        apps.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, apps.size());

                        // 4. Refresh Parent Activity
                        if (getActivity() instanceof PrioritySetupActivity) {
                            ((PrioritySetupActivity) getActivity()).refreshUnassignedApps();
                        }
                    }
                });

                adapter.setEditMode(isEditing);
                rvApps.setAdapter(adapter);
            });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // Full screen layout taaki translucent background pura cover kare
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);

            // 🔥 STATUS BAR THEME POLISH 🔥
            // Taaki translucent background ke upar icons (Time/Battery) saaf dikhein
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decorView);

                // Kyunki hamara background translucent black (#9C000000) hai,
                // toh humein hamesha LIGHT icons (white) chahiye honge status bar par.
                controller.setAppearanceLightStatusBars(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}