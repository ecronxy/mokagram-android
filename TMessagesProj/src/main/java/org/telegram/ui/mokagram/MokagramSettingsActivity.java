package org.telegram.ui.mokagram;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.SettingsActivity;

import java.util.ArrayList;

// Mokagram: корень настроек как в mokaGram Desktop: категории с цветными плитками, внутри страницы.
public final class MokagramSettingsActivity extends UniversalFragment {
    @Override
    public android.view.View createView(android.content.Context context) {
        final android.view.View root = super.createView(context);
        listView.setSections(org.telegram.messenger.AndroidUtilities.dp(16), org.telegram.messenger.AndroidUtilities.dp(22), false);
        actionBar.setCastShadows(false);
        return root;
    }

    @Override
    protected CharSequence getTitle() {
        return "Mokagram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(SettingsActivity.SettingCell.Factory.of(1, IconBackgroundColors.GRAY.top, IconBackgroundColors.GRAY.bottom, R.drawable.settings_privacy, LocaleController.getString(R.string.MokagramPrivacy)));
        items.add(UItem.asShadow(null));
        items.add(SettingsActivity.SettingCell.Factory.of(200, IconBackgroundColors.BLUE.top, IconBackgroundColors.BLUE.bottom, R.drawable.settings_account, LocaleController.getString(R.string.MokagramSource)));
        items.add(SettingsActivity.SettingCell.Factory.of(201, IconBackgroundColors.GREEN.top, IconBackgroundColors.GREEN.bottom, R.drawable.settings_chat, LocaleController.getString(R.string.MokagramAuthor)));
        items.add(SettingsActivity.SettingCell.Factory.of(202, IconBackgroundColors.BLUE_DEEP.top, IconBackgroundColors.BLUE_DEEP.bottom, R.drawable.settings_data, "mokaGram Desktop"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            presentFragment(new MokagramPrivacyActivity());
        } else if (item.id == 200) {
            Browser.openUrl(getParentActivity(), "https://github.com/ecronxy/mokagram-android");
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
