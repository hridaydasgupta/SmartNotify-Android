package com.smartnotify.app.ui.setup;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;
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

public class PrioritySetupActivity extends AppCompatActivity {

    private PriorityViewModel viewModel;
    private UnassignedAppsAdapter adapter;
    private final List<AppInfoModel> unassignedList = new ArrayList<>();

    private TextView tvFocusTime;
    private TextView tvLowPriorityTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_priority_setup);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // ViewModel setup
        viewModel = new ViewModelProvider(this).get(PriorityViewModel.class);

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
    }

    private void setupRecyclerView() {
        RecyclerView rvUnassignedApps = findViewById(R.id.rvUnassignedApps);
        rvUnassignedApps.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Tumhara purana adapter class idhar connect hoga
        adapter = new UnassignedAppsAdapter(unassignedList, app -> {
            // Normal click event (if needed in future)
        });
        rvUnassignedApps.setAdapter(adapter);
    }

    // Yeh check karega ki agar pop-up pehle se screen par hai, toh dusra mat kholo
    private void showDialogIfSafe(int priority) {
        if (getSupportFragmentManager().findFragmentByTag("PriorityPreviewDialog") == null) {
            PriorityPreviewDialog.show(getSupportFragmentManager(), priority);
        }
    }

    private void setupRingClicks() {
        findViewById(R.id.imgHighRing).setOnClickListener(v -> showDialogIfSafe(PriorityConstants.HIGH));
        findViewById(R.id.imgMediumRing).setOnClickListener(v -> showDialogIfSafe(PriorityConstants.MEDIUM));
        findViewById(R.id.imgLowRing).setOnClickListener(v -> showDialogIfSafe(PriorityConstants.LOW));
    }

    // Yeh ek naya function hai jo Dialog band hone par Unassigned List ko wapas refresh karega
    public void refreshUnassignedApps() {
        if (viewModel != null) {
            viewModel.loadInstalledApps();
        }
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

                    // UI se hata do instantly
                    unassignedList.remove(app);
                    adapter.notifyDataSetChanged();

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
}