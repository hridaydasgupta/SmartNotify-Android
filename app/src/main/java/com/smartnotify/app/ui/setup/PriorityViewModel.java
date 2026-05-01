package com.smartnotify.app.ui.setup;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.model.AppInfoModel;
import com.smartnotify.app.util.PriorityConstants;

import java.util.ArrayList;
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
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<AppInfoModel>> getUnassignedApps() {
        return unassignedApps;
    }

    public void loadInstalledApps() {
        executorService.execute(() -> {
            PackageManager pm = getApplication().getPackageManager();
            List<AppInfoModel> models = new ArrayList<>();

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

            Set<String> addedPackages = new HashSet<>();

            for (ResolveInfo resolveInfo : resolveInfos) {
                String pkgName = resolveInfo.activityInfo.packageName;

                // Apni khud ki "Smart Notify" app ko list me mat dikhao
                if (pkgName.equals(getApplication().getPackageName())) {
                    continue;
                }

                if (!addedPackages.contains(pkgName)) {
                    int savedPriority = repository.getAppPriority(pkgName);

                    if (savedPriority == PriorityConstants.UNASSIGNED) {
                        try {
                            // 🔥 FIX YAHAN HAI: Ab hum Activity nahi, balki original Application ki details nikalenge
                            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);

                            models.add(new AppInfoModel(
                                    pkgName,
                                    pm.getApplicationLabel(appInfo).toString(), // Original App Name (e.g., Google)
                                    pm.getApplicationIcon(appInfo)              // Original App Icon
                            ));
                            addedPackages.add(pkgName);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            unassignedApps.postValue(models);
        });
    }

    public void assignAppPriority(String packageName, int priority) {
        repository.saveAppPriority(packageName, priority);
    }

    public NotificationRepository getRepository() {
        return repository;
    }
}