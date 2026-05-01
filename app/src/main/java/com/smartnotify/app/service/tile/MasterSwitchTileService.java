package com.smartnotify.app.service.tile;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.smartnotify.app.data.local.entity.NotificationEntity;
import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.util.NotificationHelper;
import com.smartnotify.app.util.PriorityConstants;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N) // Quick Settings Tile Android 7.0 (Nougat) se aage hi support karta hai
public class MasterSwitchTileService extends TileService {

    private NotificationRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new NotificationRepository(this);
    }

    // Jab user notification panel neeche kheechega, yeh method call hoga update karne ke liye
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    // Jab user is button par click (Tap) karega
    @Override
    public void onClick() {
        super.onClick();

        // Current state check karo aur usko ulta (toggle) kar do
        boolean isCurrentlyActive = repository.getPreferences().isMasterSwitchActive();
        boolean newState = !isCurrentlyActive;

        repository.getPreferences().setMasterSwitch(newState);

        if (!newState) {
            // Agar user ne OFF kiya hai, toh saare ruke hue notifications ko azaad kar do!
            Log.d("SmartNotify", "Master Switch OFF - Releasing all pending notifications");
            releaseAllPendingNotifications();
        } else {
            Log.d("SmartNotify", "Master Switch ON - Strict Mode Activated");
        }

        // Tile ka UI update karo (Color/Text change karo)
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile != null) {
            boolean isActive = repository.getPreferences().isMasterSwitchActive();
            if (isActive) {
                tile.setState(Tile.STATE_ACTIVE); // Active matlab Highlighted/Colored dikhega
                tile.setLabel("Smart Notify");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle("Focus ON"); // Subtitles Android 10+ me aate hain
                }
            } else {
                tile.setState(Tile.STATE_INACTIVE); // Inactive matlab Grey out ho jayega
                tile.setLabel("Smart Notify");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle("Paused");
                }
            }
            tile.updateTile();
        }
    }

    private void releaseAllPendingNotifications() {
        // Medium aur Low dono priorities ke pending notifications DB se uthao
        List<NotificationEntity> mediumNotifs = repository.getPendingNotifications(PriorityConstants.MEDIUM);
        List<NotificationEntity> lowNotifs = repository.getPendingNotifications(PriorityConstants.LOW);

        // Dono ko humare Zero-Lag Helper me bhej do! (Wo background me handle kar lega)
        if (!mediumNotifs.isEmpty()) {
            NotificationHelper.deliverNotificationsZeroLag(this, mediumNotifs, repository, PriorityConstants.MEDIUM);
        }

        if (!lowNotifs.isEmpty()) {
            NotificationHelper.deliverNotificationsZeroLag(this, lowNotifs, repository, PriorityConstants.LOW);
        }
    }
}