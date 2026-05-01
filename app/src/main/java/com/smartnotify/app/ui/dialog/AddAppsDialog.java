package com.smartnotify.app.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartnotify.app.R;
import com.smartnotify.app.ui.adapter.AddAppsAdapter;
import com.smartnotify.app.ui.setup.PriorityViewModel;

import java.util.ArrayList;
import java.util.Set;

public class AddAppsDialog extends DialogFragment {

    private static final String ARG_PRIORITY = "arg_priority";
    private int priority;
    private PriorityViewModel viewModel;
    private AddAppsAdapter adapter;
    private OnAppsAddedListener listener;

    // Interface taaki purane Dialog ko pata chale ki nayi apps add hui hain
    public interface OnAppsAddedListener {
        void onAppsAdded();
    }

    public void setListener(OnAppsAddedListener listener) {
        this.listener = listener;
    }

    public static AddAppsDialog newInstance(int priority) {
        AddAppsDialog dialog = new AddAppsDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_PRIORITY, priority);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Is dialog ko bhi transparent background dena hai
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            priority = getArguments().getInt(ARG_PRIORITY);
        }

        // Humara naya multi-select XML
        View view = inflater.inflate(R.layout.dialog_add_apps, container, false);

        RecyclerView rvAddApps = view.findViewById(R.id.rvAddApps);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnAddSelected = view.findViewById(R.id.btnAddSelected);

        rvAddApps.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🔥 MAGIC: Hum directly Activity ka ViewModel use kar rahe hain,
        // taaki data fat-a-fat wahi se uth kar aa jaye bina dobara load kiye!
        viewModel = new ViewModelProvider(requireActivity()).get(PriorityViewModel.class);

        // ViewModel se Unassigned Apps list observe karna
        viewModel.getUnassignedApps().observe(getViewLifecycleOwner(), apps -> {
            adapter = new AddAppsAdapter(apps, count -> {
                // Button ka text update karo (e.g., "Add (2)")
                if (count > 0) {
                    btnAddSelected.setText("Add (" + count + ")");
                } else {
                    btnAddSelected.setText("Add (0)");
                }
            });
            rvAddApps.setAdapter(adapter);
        });

        btnCancel.setOnClickListener(v -> dismiss());

        // Jab "Add" pe click ho
        // Jab "Add" pe click ho
        btnAddSelected.setOnClickListener(v -> {
            if (adapter != null) {
                Set<String> selected = adapter.getSelectedApps();
                if (!selected.isEmpty()) {

                    // 1. Har selected app ko DB me save karo (Background me jayega)
                    for (String pkg : selected) {
                        viewModel.assignAppPriority(pkg, priority);
                    }

                    // 🔥 RACE CONDITION FIX: Thoda delay lagayenge taaki DB pehle saving complete kar le 🔥
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

                        // Parent Dialog (Grid) ko refresh karne bolo
                        if (listener != null) {
                            listener.onAppsAdded();
                        }

                        // Main Activity ki bottom list ko bhi refresh kardo
                        viewModel.loadInstalledApps();

                        dismiss(); // Dialog band kardo

                    }, 250); // Sirf 250 milliseconds (0.25 sec) ka wait

                } else {
                    Toast.makeText(getContext(), "Please select at least one app", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Background click pe band
        view.setOnClickListener(v -> dismiss());

        return view;
    }
}