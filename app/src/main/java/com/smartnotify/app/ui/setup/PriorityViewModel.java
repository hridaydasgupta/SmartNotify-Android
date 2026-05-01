package com.smartnotify.app.ui.setup;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.model.AppInfoModel;
import com.smartnotify.app.util.PriorityConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PriorityViewModel extends AndroidViewModel {

    private final NotificationRepository repository;
    private final MutableLiveData<List<AppInfoModel>> unassignedApps = new MutableLiveData<>();
    private final ExecutorService executorService;

    public PriorityViewModel(@NonNull Application application) {
        super(application);
        repository = new NotificationRepository(application);
        // Single thread executor background tasks ke liye sahi hai
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<AppInfoModel>> getUnassignedApps() {
        return unassignedApps;
    }

    public void loadInstalledApps() {
        executorService.execute(() -> {
            PackageManager pm = getApplication().getPackageManager();
            List<AppInfoModel> models = new ArrayList<>();

            // 1. Database se saari assigned packages ek hi baar mein nikal lo (Optimization)
            // Note: Iske liye Repository mein getAllAssignedPackageNames() method hona chahiye
            Set<String> assignedPackages = new HashSet<>(repository.getAssignedPackageNamesSync());

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

            Set<String> processedPackages = new HashSet<>();

            for (ResolveInfo resolveInfo : resolveInfos) {
                String pkgName = resolveInfo.activityInfo.packageName;

                // Apni app aur already assigned apps ko skip karo
                if (pkgName.equals(getApplication().getPackageName()) || assignedPackages.contains(pkgName)) {
                    continue;
                }

                if (!processedPackages.contains(pkgName)) {
                    try {
                        ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
                        String appLabel = pm.getApplicationLabel(appInfo).toString();
                        Drawable icon = pm.getApplicationIcon(appInfo);

                        models.add(new AppInfoModel(pkgName, appLabel, icon));
                        processedPackages.add(pkgName);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 2. Alphabetical Sort: Taaki apps A to Z dikhein (Better UX)
            Collections.sort(models, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));

            unassignedApps.postValue(models);
        });
    }

    public void assignAppPriority(String packageName, int priority) {
        executorService.execute(() -> {
            repository.saveAppPriority(packageName, priority);
            // Assign karne ke baad list refresh karna zaroori hai
            loadInstalledApps();
        });
    }

    public NotificationRepository getRepository() {
        return repository;
    }
}