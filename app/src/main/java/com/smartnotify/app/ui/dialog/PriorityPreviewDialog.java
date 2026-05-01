package com.smartnotify.app.ui.dialog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;
import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.ui.adapter.PriorityPreviewAdapter;
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
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
        repository = new NotificationRepository(requireContext());
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
        RecyclerView rvApps = view.findViewById(R.id.rvApps);

        if (priority == PriorityConstants.HIGH) {
            tvTitle.setText("High Priority");
        } else if (priority == PriorityConstants.MEDIUM) {
            tvTitle.setText("Medium Priority");
        } else {
            tvTitle.setText("Low Priority");
        }

        rvApps.setLayoutManager(new GridLayoutManager(getContext(), 4));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        // 🔥 PEHLI JAGAH (Initial Load) 🔥
        executor.execute(() -> {
            List<String> apps = repository.getAppsByPriority(priority);
            handler.post(() -> {
                adapter = new PriorityPreviewAdapter(requireContext(), apps, (packageName, position) -> {
                    // 🔥 CRASH FIX ADDED HERE 🔥
                    if (position >= 0 && position < apps.size()) {
                        repository.saveAppPriority(packageName, PriorityConstants.UNASSIGNED);
                        apps.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, apps.size());

                        if (getActivity() instanceof com.smartnotify.app.ui.setup.PrioritySetupActivity) {
                            ((com.smartnotify.app.ui.setup.PrioritySetupActivity) getActivity()).refreshUnassignedApps();
                        }
                    }
                });
                rvApps.setAdapter(adapter);
            });
        });

        // 🔥 EDIT BUTTON LOGIC 🔥
        tvEdit.setOnClickListener(v -> {
            isEditing = !isEditing; // State ko flip karo (True ko False, False ko True)

            if (isEditing) {
                tvEdit.setText("Done"); // Agar edit mode me hai toh text "Done" kar do
                btnAddApps.setVisibility(View.GONE); // 🔥 FIX: Plus icon ko hide kar do
            } else {
                tvEdit.setText("Edit"); // Wapas normal "Edit" kar do
                btnAddApps.setVisibility(View.VISIBLE); // 🔥 FIX: Plus icon ko wapas le aao
            }

            if (adapter != null) {
                adapter.setEditMode(isEditing); // Adapter ko batao mode change ho gaya hai
            }
        });

        btnAddApps.setOnClickListener(v -> {
            AddAppsDialog dialog = AddAppsDialog.newInstance(priority);
            dialog.setListener(() -> {
                executor.execute(() -> {
                    List<String> updatedApps = repository.getAppsByPriority(priority);
                    handler.post(() -> {
                        // 🔥 DUSRI JAGAH (Refresh on Add) 🔥
                        adapter = new PriorityPreviewAdapter(requireContext(), updatedApps, (packageName, position) -> {
                            // 🔥 CRASH FIX ADDED HERE TOO 🔥
                            if (position >= 0 && position < updatedApps.size()) {
                                repository.saveAppPriority(packageName, PriorityConstants.UNASSIGNED);
                                updatedApps.remove(position);
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position, updatedApps.size());

                                if (getActivity() instanceof com.smartnotify.app.ui.setup.PrioritySetupActivity) {
                                    ((com.smartnotify.app.ui.setup.PrioritySetupActivity) getActivity()).refreshUnassignedApps();
                                }
                            }
                        });
                        adapter.setEditMode(isEditing);
                        rvApps.setAdapter(adapter);
                    });
                });
            });
            dialog.show(getChildFragmentManager(), "AddAppsDialog");
        });

        view.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}