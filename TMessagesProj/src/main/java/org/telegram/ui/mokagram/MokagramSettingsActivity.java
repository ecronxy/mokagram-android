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

public final class MokagramSettingsActivity extends UniversalFragment {
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
    protected CharSequence getTitle() {
        return "Mokagram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.MokagramGhost)));
        for (int i = 0; i < 6; i++) {
            addToggle(items, i);
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.MokagramGhostInfo)));
        items.add(UItem.asHeader(LocaleController.getString(R.string.MokagramSchedule)));
        addToggle(items, 6);
        items.add(UItem.asButton(100, LocaleController.getString(R.string.MokagramScheduleFrom), time("ghost.schedule_from", 1380)));
        items.add(UItem.asButton(101, LocaleController.getString(R.string.MokagramScheduleTo), time("ghost.schedule_to", 420)));
        for (int i = 7; i < KEYS.length; i++) {
            addToggle(items, i);
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.MokagramScheduleInfo)));
        items.add(UItem.asHeader(LocaleController.getString(R.string.MokagramAbout)));
        items.add(UItem.asButton(200, LocaleController.getString(R.string.MokagramSource)));
        items.add(UItem.asButton(201, LocaleController.getString(R.string.MokagramAuthor)));
        items.add(UItem.asButton(202, "mokaGram Desktop — ecronxy/mokagram"));
        items.add(UItem.asShadow(LocaleController.getString(R.string.MokagramAttribution)));
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
        } else if (item.id == 100 || item.id == 101) {
            String key = item.id == 100 ? "ghost.schedule_from" : "ghost.schedule_to";
            int minute = MokagramSettings.minute(key, item.id == 100 ? 1380 : 420);
            new TimePickerDialog(getParentActivity(), (picker, hour, minutes) -> {
                MokagramSettings.setMinute(key, hour * 60 + minutes);
                listView.adapter.update(true);
            }, minute / 60, minute % 60, true).show();
        } else if (item.id == 200) {
            Browser.openUrl(getParentActivity(), "https://github.com/kapustaltd/mokagram-android");
        } else if (item.id == 201) {
            Browser.openUrl(getParentActivity(), "https://t.me/ecronx");
        } else if (item.id == 202) {
            Browser.openUrl(getParentActivity(), "https://github.com/ecronxy/mokagram");
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
