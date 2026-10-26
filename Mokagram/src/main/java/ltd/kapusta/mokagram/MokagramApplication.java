package ltd.kapusta.mokagram;

import org.telegram.messenger.ApplicationLoader;

public final class MokagramApplication extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
