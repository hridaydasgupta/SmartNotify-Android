package com.smartnotify.app.ui.setup;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.DragEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;
import com.smartnotify.app.data.prefs.UserPreferences;
import com.smartnotify.app.model.AppInfoModel;
import com.smartnotify.app.ui.adapter.UnassignedAppsAdapter;
import com.smartnotify.app.ui.dialog.PriorityPreviewDialog;
import com.smartnotify.app.util.PriorityConstants;
import com.smartnotify.app.util.TimeUtils;
import com.smartnotify.app.worker.WorkScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrioritySetupActivity extends AppCompatActivity {

    private PriorityViewModel viewModel;
    private UnassignedAppsAdapter adapter;
    private final List<AppInfoModel> unassignedList = new ArrayList<>();

    private TextView tvFocusTime;
    private TextView tvLowPriorityTime;

    // 🚀 GridLayouts for Icon Previews
    private GridLayout gridHighPreview;
    private GridLayout gridMediumPreview;
    private GridLayout gridLowPreview;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_priority_setup);

        // Notification Permission Check (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 🚀 Battery Optimization Bypass Request (VIP Status)
        requestBatteryOptimizationBypass();

        // Threads Setup for Previews
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // ViewModel setup
        viewModel = new ViewModelProvider(this).get(PriorityViewModel.class);

        // UI Binding
        gridHighPreview = findViewById(R.id.gridHighPreview);
        gridMediumPreview = findViewById(R.id.gridMediumPreview);
        gridLowPreview = findViewById(R.id.gridLowPreview);

        setupRecyclerView();
        setupDragAndDrop();
        setupTimePickers();
        setupRingClicks();

        // Observe ViewModel: Jaise hi background se apps load hongi, ye block chalega
        viewModel.getUnassignedApps().observe(this, apps -> {
            unassignedList.clear();
            unassignedList.addAll(apps);
            adapter.notifyDataSetChanged();
        });

        // App start hote hi apps load karna shuru karo
        viewModel.loadInstalledApps();

        // Initial load for Ring Previews
        refreshAllRingPreviews();
    }

    // ===================================
    // 🚀 NEW: RING PREVIEW ENGINE (Chhote Icons)
    // ===================================
    private void refreshAllRingPreviews() {
        updateSingleRingPreview(PriorityConstants.HIGH, gridHighPreview);
        updateSingleRingPreview(PriorityConstants.MEDIUM, gridMediumPreview);
        updateSingleRingPreview(PriorityConstants.LOW, gridLowPreview);
    }

    private void updateSingleRingPreview(int priorityLevel, GridLayout gridLayout) {
        executor.execute(() -> {
            // Background thread mein DB hit aur icon fetch karo
            List<String> assignedApps = viewModel.getRepository().getAppsByPriority(priorityLevel);
            PackageManager pm = getPackageManager();

            // Main UI thread par Views create karo
            mainHandler.post(() -> {
                gridLayout.removeAllViews(); // Purane icons hatao

                // 🚀 UPDATE: Ab max 9 icons dikhayenge (3x3 grid)
                int maxIcons = Math.min(assignedApps.size(), 9);

                for (int i = 0; i < maxIcons; i++) {
                    String packageName = assignedApps.get(i);
                    try {
                        Drawable icon = pm.getApplicationIcon(packageName);

                        // Chhota ImageView banao
                        ImageView imageView = new ImageView(this);
                        imageView.setImageDrawable(icon);

                        // 🚀 UPDATE: Icon size 16dp kar diya taaki 9 icons fit ho sakein
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        int sizeInPx = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                        params.width = sizeInPx;
                        params.height = sizeInPx;

                        // Thoda sa gap taaki icons aapas mein na chipkein
                        params.setMargins(2, 2, 2, 2);
                        imageView.setLayoutParams(params);

                        // Grid mein add kar do
                        gridLayout.addView(imageView);

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    // ===================================
    // BATTERY OPTIMIZATION BYPASS LOGIC
    // ===================================
    private void requestBatteryOptimizationBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvUnassignedApps = findViewById(R.id.rvUnassignedApps);
        rvUnassignedApps.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapter = new UnassignedAppsAdapter(unassignedList, app -> {
            // Normal click event (if needed in future)
        });
        rvUnassignedApps.setAdapter(adapter);
    }

    private void showDialogIfSafe(int priority) {
        if (getSupportFragmentManager().findFragmentByTag("PriorityPreviewDialog") == null) {
            PriorityPreviewDialog.show(getSupportFragmentManager(), priority);
        }
    }

    private void setupRingClicks() {
        // Ab clicks hum Ring Image par nahi, poore container/grid par layange taaki easy to click ho
        findViewById(R.id.imgHighRing).setOnClickListener(v -> showDialogIfSafe(PriorityConstants.HIGH));
        findViewById(R.id.imgMediumRing).setOnClickListener(v -> showDialogIfSafe(PriorityConstants.MEDIUM));
        findViewById(R.id.imgLowRing).setOnClickListener(v -> showDialogIfSafe(PriorityConstants.LOW));

        // Agar user chhote icons (Grid) par click kare, tab bhi dialog khulna chahiye
        gridHighPreview.setOnClickListener(v -> showDialogIfSafe(PriorityConstants.HIGH));
        gridMediumPreview.setOnClickListener(v -> showDialogIfSafe(PriorityConstants.MEDIUM));
        gridLowPreview.setOnClickListener(v -> showDialogIfSafe(PriorityConstants.LOW));
    }

    // Ye function Dialog se bhi call hoga jab koi app remove hogi
    public void refreshUnassignedApps() {
        if (viewModel != null) {
            viewModel.loadInstalledApps();
        }
        // Saath mein rings bhi update honi chahiye
        refreshAllRingPreviews();
    }

    private void setupDragAndDrop() {
        ImageView imgHighRing = findViewById(R.id.imgHighRing);
        ImageView imgMediumRing = findViewById(R.id.imgMediumRing);
        ImageView imgLowRing = findViewById(R.id.imgLowRing);

        // Tags to identify rings
        imgHighRing.setTag(PriorityConstants.HIGH);
        imgMediumRing.setTag(PriorityConstants.MEDIUM);
        imgLowRing.setTag(PriorityConstants.LOW);

        View.OnDragListener ringDragListener = (v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.6f); // Ring thodi blur hogi (visual feedback)
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1f); // Ring wapas normal
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setAlpha(1f);
                    AppInfoModel app = (AppInfoModel) event.getLocalState();
                    int priority = (int) v.getTag();

                    // Database me save karo
                    viewModel.assignAppPriority(app.getPackageName(), priority);

                    // 🚀 The Missing Link: Broadcast to update O(1) Cache in Service
                    Intent updateCacheIntent = new Intent("com.smartnotify.CACHE_UPDATE");
                    sendBroadcast(updateCacheIntent);

                    // UI se hata do instantly
                    unassignedList.remove(app);
                    adapter.notifyDataSetChanged();

                    // 🚀 Naya icon ring mein update karo
                    refreshAllRingPreviews();

                    Toast.makeText(this, app.getAppName() + " assigned!", Toast.LENGTH_SHORT).show();
                    return true;
            }
            return true;
        };

        imgHighRing.setOnDragListener(ringDragListener);
        imgMediumRing.setOnDragListener(ringDragListener);
        imgLowRing.setOnDragListener(ringDragListener);
    }

    // ===================================
    // TIME PICKER & WORK MANAGER LOGIC
    // ===================================
    private void setupTimePickers() {
        tvFocusTime = findViewById(R.id.tvFocusTime);
        tvLowPriorityTime = findViewById(R.id.tvLowPriorityTime);

        UserPreferences prefs = viewModel.getRepository().getPreferences();

        // Pehle se save kiye hue times UI pe dikhao
        updateTimeUI(tvFocusTime, prefs.getFocusStartTime(), prefs.getFocusEndTime());
        updateTimeUI(tvLowPriorityTime, prefs.getLowPriorityStartTime(), prefs.getLowPriorityEndTime());

        // Focus Time pe click (Medium Priority)
        findViewById(R.id.tvFocusTime).setOnClickListener(v -> {
            openTimePicker("Focus Start Time", (startHour, startMin) -> {
                String startTime = formatTime(startHour, startMin);

                openTimePicker("Focus End Time (Release Time)", (endHour, endMin) -> {
                    String endTime = formatTime(endHour, endMin);

                    // Save to Preferences
                    prefs.setFocusStartTime(startTime);
                    prefs.setFocusEndTime(endTime);
                    updateTimeUI(tvFocusTime, startTime, endTime);

                    // 🚀 The Magic: WorkManager Alarm Set Karo!
                    WorkScheduler.scheduleMediumPriorityRelease(this, endTime);
                    Toast.makeText(this, "Focus Time & Release Alarm Set!", Toast.LENGTH_SHORT).show();
                });
            });
        });

        // Low Priority Time pe click
        findViewById(R.id.tvLowPriorityTime).setOnClickListener(v -> {
            openTimePicker("Low Priority Window Start", (startHour, startMin) -> {
                String startTime = formatTime(startHour, startMin);

                openTimePicker("Low Priority Window End", (endHour, endMin) -> {
                    String endTime = formatTime(endHour, endMin);

                    // Save to Preferences
                    prefs.setLowPriorityStartTime(startTime);
                    prefs.setLowPriorityEndTime(endTime);
                    updateTimeUI(tvLowPriorityTime, startTime, endTime);

                    // 🚀 Set the Alarm
                    WorkScheduler.scheduleLowPriorityRelease(this, startTime);
                    Toast.makeText(this, "Low Priority Window & Release Alarm Set!", Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    private void openTimePicker(String title, TimePickerCallback callback) {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            callback.onTimeSelected(hourOfDay, minute);
        }, 12, 0, false);
        dialog.setTitle(title);
        dialog.show();
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private void updateTimeUI(TextView tv, String startTime, String endTime) {
        String formattedStart = TimeUtils.formatTo12Hour(startTime);
        String formattedEnd = TimeUtils.formatTo12Hour(endTime);
        tv.setText(formattedStart + " to " + formattedEnd);
    }

    interface TimePickerCallback {
        void onTimeSelected(int hour, int minute);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBatteryPermissionStatus();
    }

    private void checkBatteryPermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "⚠️ Warning: App may stop working in background. Please allow battery bypass.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}