package ltd.kapusta.mokagram;

import android.graphics.Typeface;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import org.telegram.messenger.ApplicationLoader;

import java.lang.reflect.Field;
import java.util.Map;

public final class MokagramApplication extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        installFont();
        org.telegram.messenger.mokagram.Badges.start();
    }

    // Nunito вместо системного Roboto во всём клиенте (как шрифт из exteraGram)
    private void installFont() {
        try {
            HiddenApiBypass.addHiddenApiExemptions("L");
            Typeface regular = Typeface.createFromAsset(getAssets(), "fonts/nunito_regular.ttf");
            Typeface medium = Typeface.createFromAsset(getAssets(), "fonts/rmedium.ttf");
            setStatic("DEFAULT", regular);
            setStatic("SANS_SERIF", regular);
            setStatic("DEFAULT_BOLD", medium);
            Field mapField = Typeface.class.getDeclaredField("sSystemFontMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Typeface> map = (Map<String, Typeface>) mapField.get(null);
            map.put("sans-serif", regular);
            map.put("sans-serif-medium", medium);
            map.put("sans-serif-bold", medium);
            android.util.Log.i("MokaFont", "installed");
        } catch (Throwable e) {
            android.util.Log.e("MokaFont", "failed", e);
        }
    }

    private static void setStatic(String name, Typeface value) throws Exception {
        Field field = Typeface.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
