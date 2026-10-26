package org.telegram.messenger.mokagram;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.tl.TL_account;

import java.util.Calendar;

public final class MokagramSettings {
    private MokagramSettings() {
    }

    private static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("mokagram", Context.MODE_PRIVATE);
    }

    public static boolean get(String key, boolean fallback) {
        return preferences().getBoolean(key, fallback);
    }

    public static void set(String key, boolean value) {
        boolean wasOnlineHidden = hides(GhostPolicy.Event.ONLINE);
        preferences().edit().putBoolean(key, value).apply();
        if (!wasOnlineHidden && hides(GhostPolicy.Event.ONLINE) && get("ghost.offline_on_enable", true)) {
            sendOffline();
        }
    }

    public static int minute(String key, int fallback) {
        return preferences().getInt(key, fallback);
    }

    public static void setMinute(String key, int value) {
        if (value < 0 || value >= 1440) {
            throw new IllegalArgumentException("Time must be minutes since midnight");
        }
        preferences().edit().putInt(key, value).apply();
    }

    public static boolean hides(GhostPolicy.Event event) {
        String suffix;
        switch (event) {
            case READ: suffix = "read"; break;
            case ONLINE: suffix = "online"; break;
            case TYPING: suffix = "typing"; break;
            case STORY: suffix = "stories"; break;
            default: throw new IllegalArgumentException();
        }
        Calendar now = Calendar.getInstance();
        boolean scheduled = get("ghost.schedule", false) && GhostPolicy.insideSchedule(
                now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE),
                minute("ghost.schedule_from", 23 * 60), minute("ghost.schedule_to", 7 * 60));
        return GhostPolicy.hides(get("ghost.enabled", false), get("ghost." + suffix, true),
                scheduled, get("ghost.sched_" + suffix, event != GhostPolicy.Event.STORY));
    }

    public static void sendOffline() {
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (UserConfig.getInstance(account).isClientActivated()) {
                TL_account.updateStatus request = new TL_account.updateStatus();
                request.offline = true;
                ConnectionsManager.getInstance(account).sendRequest(request, null);
            }
        }
    }

}
