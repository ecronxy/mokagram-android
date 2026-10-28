package org.telegram.ui.mokagram;

import android.app.TimePickerDialog;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.mokagram.MokagramSettings;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Locale;

public final class MokagramPrivacyActivity extends UniversalFragment {
    private static final String[] KEYS = {
            "ghost.enabled", "ghost.read", "ghost.online", "ghost.typing", "ghost.stories",
            "ghost.offline_on_enable", "ghost.schedule", "ghost.sched_read", "ghost.sched_online",
            "ghost.sched_typing", "ghost.sched_stories"
    };
    private static final int[] TITLES = {
            R.string.MokagramGhost, R.string.MokagramGhostRead, R.string.MokagramGhostOnline,
            R.string.MokagramGhostTyping, R.string.MokagramGhostStories, R.string.MokagramGhostOffline,
            R.string.MokagramSchedule, R.string.MokagramGhostRead, R.string.MokagramGhostOnline,
            R.string.MokagramGhostTyping, R.string.MokagramGhostStories
    };

    @Override
    public android.view.View createView(android.content.Context context) {
        final android.view.View root = super.createView(context);
        listView.setSections(org.telegram.messenger.AndroidUtilities.dp(16), org.telegram.messenger.AndroidUtilities.dp(22), false);
        listView.setItemAnimator(null); // иначе при скрытии строк под скруглёнными углами мелькают квадратные
        actionBar.setCastShadows(false);
        return root;
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString(R.string.MokagramPrivacy);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.MokagramGhost)));
        addToggle(items, 0);
        // Функции призрака видны только при включённом призраке
        if (MokagramSettings.get(KEYS[0], false)) {
            for (int i = 1; i < 6; i++) {
                addToggle(items, i);
            }
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(LocaleController.getString(R.string.MokagramSchedule)));
        addToggle(items, 6);
        // Время и функции расписания видны только при включённом расписании
        if (MokagramSettings.get(KEYS[6], false)) {
            items.add(UItem.asButton(100, LocaleController.getString(R.string.MokagramScheduleTime), String.format(Locale.US, "%s – %s", time("ghost.schedule_from", 1380), time("ghost.schedule_to", 420))));
            for (int i = 7; i < KEYS.length; i++) {
                addToggle(items, i);
            }
        }
        items.add(UItem.asShadow(null));
    }

    private void addToggle(ArrayList<UItem> items, int index) {
        boolean fallback = index != 0 && index != 6 && index != 10;
        items.add(UItem.asCheck(index + 1, LocaleController.getString(TITLES[index]))
                .setChecked(MokagramSettings.get(KEYS[index], fallback)));
    }

    private String time(String key, int fallback) {
        int minute = MokagramSettings.minute(key, fallback);
        return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 1 && item.id <= KEYS.length) {
            MokagramSettings.set(KEYS[item.id - 1], !item.checked);
            listView.adapter.update(true);
        } else if (item.id == 100) {
            int from = MokagramSettings.minute("ghost.schedule_from", 1380);
            new TimePickerDialog(getParentActivity(), (pickerFrom, hourFrom, minutesFrom) -> {
                MokagramSettings.setMinute("ghost.schedule_from", hourFrom * 60 + minutesFrom);
                int to = MokagramSettings.minute("ghost.schedule_to", 420);
                new TimePickerDialog(getParentActivity(), (pickerTo, hourTo, minutesTo) -> {
                    MokagramSettings.setMinute("ghost.schedule_to", hourTo * 60 + minutesTo);
                    listView.adapter.update(true);
                }, to / 60, to % 60, true).show();
            }, from / 60, from % 60, true).show();
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
