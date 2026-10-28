package org.telegram.messenger.mokagram;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_bots;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Галочки mokaGram, как в десктопе: список выдаёт бот @mokagent inline-запросами «badges:N»
// (страницами по 44), значок и подпись подмешиваются как «проверка ботом» (bot_verification).
public final class Badges {
    private static final long OWNER_ID = 6612638154L;
    private static final long DEFAULT_ICON = 5402618655601109041L;
    private static final long BLUE_ICON = 5397879498492389616L;
    private static final String BOT_USERNAME = "mokagent";
    private static final int PAGE_SIZE = 44;
    private static final String[] TEAM = {"litteraliy", "xdshka"};

    private static final class Custom {
        long icon;
        String text;
    }

    private static final Map<Long, Custom> custom = new ConcurrentHashMap<>();
    private static final Set<Long> remote = ConcurrentHashMap.newKeySet();
    private static final Set<Long> blue = ConcurrentHashMap.newKeySet();
    private static final Set<Long> applied = ConcurrentHashMap.newKeySet();
    private static volatile boolean apiOk;
    private static boolean sseStarted;
    private static volatile boolean sseAlive;
    private static int apiFails;
    private static String etag;
    private static String lastEventId;
    private static final String API = "https://api.moka.wtf";
    private static volatile long icon = DEFAULT_ICON;
    private static volatile boolean fresh;
    private static boolean loaded;
    private static boolean inFlight;
    private static boolean started;
    private static TLRPC.InputUser botUser;
    private static final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private Badges() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("mokagram", Context.MODE_PRIVATE);
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            final String cached = prefs().getString("badges.cache", null);
            if (cached == null) {
                return;
            }
            final JSONObject object = new JSONObject(cached);
            final long cachedIcon = object.optLong("emoji_id", 0);
            if (cachedIcon != 0) {
                icon = cachedIcon;
            }
            final JSONArray users = object.optJSONArray("users");
            for (int i = 0; users != null && i < users.length(); ++i) {
                remote.add(users.getLong(i));
            }
            final JSONArray rows = object.optJSONArray("custom");
            for (int i = 0; rows != null && i < rows.length(); ++i) {
                final JSONArray row = rows.getJSONArray(i);
                final Custom c = new Custom();
                c.icon = row.optLong(1, 0);
                c.text = row.optString(2, "");
                custom.put(row.getLong(0), c);
            }
        } catch (Exception ignore) {
        }
    }

    private static boolean russian() {
        return LocaleController.getInstance().getCurrentLocale() != null
                && "ru".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
    }

    private static boolean isTeam(TLRPC.User user) {
        final ArrayList<String> names = new ArrayList<>();
        if (user.username != null) {
            names.add(user.username.toLowerCase());
        }
        if (user.usernames != null) {
            for (TLRPC.TL_username u : user.usernames) {
                if (u.username != null) {
                    names.add(u.username.toLowerCase());
                }
            }
        }
        for (String team : TEAM) {
            if (names.contains(team)) {
                return true;
            }
        }
        return false;
    }

    private static String role(TLRPC.User user) {
        if (user.id == OWNER_ID) {
            return russian() ? "Разработчик и владелец mokaGram" : "Developer and owner of mokaGram";
        }
        if (isTeam(user)) {
            return russian() ? "Разработчик mokaGram" : "Maintainer mokaGram";
        }
        return null;
    }

    private static long iconFor(TLRPC.User user) {
        if (user.id == OWNER_ID || isTeam(user) || blue.contains(user.id)) {
            return BLUE_ICON;
        }
        final Custom c = custom.get(user.id);
        return c != null && c.icon != 0 ? c.icon : icon;
    }

    private static boolean has(TLRPC.User user) {
        return user.id == OWNER_ID || isTeam(user) || remote.contains(user.id);
    }

    // Значок рядом с именем: список чатов, шапка чата, профиль.
    public static void apply(TLRPC.User user) {
        if (user == null || user.bot_verification_icon != 0) {
            return;
        }
        load();
        if (has(user)) {
            user.bot_verification_icon = iconFor(user);
            applied.add(user.id);
        }
    }

    // Подпись под профилем.
    public static void applyFull(TLRPC.User user, TLRPC.UserFull full) {
        if (user == null || full == null || full.bot_verification != null) {
            return;
        }
        load();
        if (!has(user)) {
            return;
        }
        String text = role(user);
        if (text == null) {
            final Custom c = custom.get(user.id);
            text = c != null ? c.text : null;
        }
        if (text == null || text.isEmpty()) {
            return;
        }
        final TL_bots.BotVerification verification = new TL_bots.BotVerification();
        verification.bot_id = 0;
        verification.icon = iconFor(user);
        verification.description = new TLRPC.TL_textWithEntities();
        verification.description.text = text;
        full.bot_verification = verification;
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        load();
        tick();
    }

    private static void tick() {
        final int account = UserConfig.selectedAccount;
        android.util.Log.i("MokaBadges", "tick acc=" + account + " activated=" + UserConfig.getInstance(account).isClientActivated() + " apiOk=" + apiOk + " sse=" + sseAlive);
        loadApi(account);
        if (UserConfig.getInstance(account).isClientActivated() && !apiOk) {
            refresh(account); // запасной путь: inline-бот
        }
        // Поток жив: резервный опрос раз в 5 минут; иначе первые попытки каждые 5 с, дальше раз в минуту.
        if (!apiOk) {
            ++apiFails;
        } else {
            apiFails = 0;
        }
        handler.postDelayed(Badges::tick, apiOk && sseAlive ? 300000 : (apiFails < 3 ? 5000 : 60000));
    }

    private static final class Pages {
        final ArrayList<Long> ids = new ArrayList<>();
        final Map<Long, Custom> custom = new ConcurrentHashMap<>();
        long icon;
    }

    public interface Result {
        void done(String text);
    }

    private static void ensureBot(int account, Runnable then, Runnable fail) {
        if (botUser != null) {
            then.run();
            return;
        }
        final TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = BOT_USERNAME;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            android.util.Log.i("MokaBadges", "resolve -> " + (response == null ? "null" : response.getClass().getSimpleName()) + " err=" + (error == null ? "-" : error.text));
            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                final TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) response;
                MessagesController.getInstance(account).putUsers(peer.users, false);
                for (TLRPC.User u : peer.users) {
                    if (u.username != null && BOT_USERNAME.equalsIgnoreCase(u.username)) {
                        botUser = MessagesController.getInstance(account).getInputUser(u);
                    }
                }
            }
            if (botUser != null) {
                then.run();
            } else {
                fail.run();
            }
        });
    }

    private static String prettyDate(String raw) {
        final String[] ru = {"январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"};
        final String[] en = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        final String[] parts = raw.split("\\.");
        if (parts.length == 2) {
            try {
                final int month = Integer.parseInt(parts[0]);
                if (month >= 1 && month <= 12) {
                    return (russian() ? ru : en)[month - 1] + " " + parts[1];
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return raw;
    }

    // Дата регистрации через бота (ключ DateReg лежит на сервере бота, не в приложении).
    public static void regDate(int account, long userId, Result result) {
        final String cached = prefs().getString("regdate." + userId, null);
        if (cached != null) {
            result.done(cached);
            return;
        }
        ensureBot(account, () -> {
            final TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.bot = botUser;
            req.peer = new TLRPC.TL_inputPeerSelf();
            req.query = "dr:" + userId;
            req.offset = "";
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
                String text = null;
                if (response instanceof TLRPC.TL_messages_botResults) {
                    for (TLRPC.BotInlineResult r : ((TLRPC.TL_messages_botResults) response).results) {
                        if ("ok".equals(r.id) && r.title != null) {
                            text = prettyDate(r.title);
                        }
                    }
                }
                final String value = text;
                if (value != null) {
                    prefs().edit().putString("regdate." + userId, value).apply();
                }
                handler.post(() -> result.done(value));
            });
        }, () -> handler.post(() -> result.done(null)));
    }

    private static void refresh(int account) {
        android.util.Log.i("MokaBadges", "refresh acc=" + account + " bot=" + (botUser != null));
        synchronized (Badges.class) {
            if (inFlight) {
                return;
            }
            inFlight = true;
        }
        handler.postDelayed(() -> {
            synchronized (Badges.class) {
                inFlight = false;
            }
        }, 20000);
        if (botUser == null) {
            final TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = BOT_USERNAME;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
                android.util.Log.i("MokaBadges", "resolve -> " + (response == null ? "null" : response.getClass().getSimpleName()) + " err=" + (error == null ? "-" : error.text));
                if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                    final TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) response;
                    MessagesController.getInstance(account).putUsers(peer.users, false);
                    for (TLRPC.User u : peer.users) {
                        if (u.username != null && BOT_USERNAME.equalsIgnoreCase(u.username)) {
                            botUser = MessagesController.getInstance(account).getInputUser(u);
                        }
                    }
                }
                if (botUser == null) {
                    synchronized (Badges.class) {
                        inFlight = false;
                    }
                    return;
                }
                page(account, 0, new Pages());
            });
            return;
        }
        page(account, 0, new Pages());
    }

    private static void page(int account, int page, Pages pages) {
        final TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
        req.bot = botUser;
        req.peer = new TLRPC.TL_inputPeerSelf();
        req.query = "badges:" + page;
        req.offset = "";
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            android.util.Log.i("MokaBadges", "page " + page + " -> " + (response == null ? "null" : response.getClass().getSimpleName()) + " err=" + (error == null ? "-" : error.text));
            if (!(response instanceof TLRPC.TL_messages_botResults)) {
                synchronized (Badges.class) {
                    inFlight = false;
                }
                return;
            }
            int count = 0;
            for (TLRPC.BotInlineResult result : ((TLRPC.TL_messages_botResults) response).results) {
                try {
                    if ("e".equals(result.id)) {
                        pages.icon = Long.parseLong(result.title);
                    } else if (result.id != null && result.id.startsWith("b")) {
                        ++count;
                        final long user = Long.parseLong(result.id.substring(1));
                        pages.ids.add(user);
                        final Custom c = new Custom();
                        try {
                            c.icon = Long.parseLong(result.title);
                        } catch (Exception ignore) {
                        }
                        c.text = result.description == null ? "" : (result.description.length() > 40 ? result.description.substring(0, 40) : result.description);
                        if (c.icon != 0 || !c.text.isEmpty()) {
                            pages.custom.put(user, c);
                        }
                    }
                } catch (Exception ignore) {
                }
            }
            if (count >= PAGE_SIZE && page < 200) {
                page(account, page + 1, pages);
                return;
            }
            finish(account, pages);
        });
    }

    private static void finish(int account, Pages pages) {
        remote.clear();
        remote.addAll(pages.ids);
        custom.clear();
        custom.putAll(pages.custom);
        if (pages.icon != 0) {
            icon = pages.icon;
        }
        fresh = true;
        synchronized (Badges.class) {
            inFlight = false;
        }
        try {
            final JSONObject object = new JSONObject();
            object.put("emoji_id", icon);
            object.put("users", new JSONArray(pages.ids));
            final JSONArray rows = new JSONArray();
            for (Map.Entry<Long, Custom> e : custom.entrySet()) {
                final JSONArray row = new JSONArray();
                row.put(e.getKey());
                row.put(e.getValue().icon);
                row.put(e.getValue().text);
                rows.put(row);
            }
            object.put("custom", rows);
            prefs().edit().putString("badges.cache", object.toString()).apply();
        } catch (Exception ignore) {
        }
        handler.post(() -> {
            final MessagesController controller = MessagesController.getInstance(account);
            for (TLRPC.User user : controller.getUsers().values()) {
                apply(user);
            }
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
    }

    // ---------- API api.moka.wtf: список + события SSE ----------

    private static final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newCachedThreadPool(r -> {
        final Thread t = new Thread(r, "moka-badges");
        t.setDaemon(true);
        return t;
    });

    private static java.net.HttpURLConnection open(String path) throws Exception {
        final java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(API + path).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "mokaGram-Android");
        return c;
    }

    private static String readAll(java.io.InputStream in) throws Exception {
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toString("UTF-8");
    }

    private static long parseId(JSONObject o, String key) {
        try {
            if (o.isNull(key)) {
                return 0;
            }
            return Long.parseLong(o.getString(key));
        } catch (Exception e) {
            return 0;
        }
    }

    private static void putBadge(JSONObject b) {
        final long userId = b.optLong("userId", 0);
        if (userId == 0) {
            return;
        }
        remote.add(userId);
        final Custom c = new Custom();
        c.icon = parseId(b, "emojiId");
        final String signature = b.isNull("signature") ? "" : b.optString("signature", "");
        c.text = signature.length() > 40 ? signature.substring(0, 40) : signature;
        if (c.icon != 0 || !c.text.isEmpty()) {
            custom.put(userId, c);
        } else {
            custom.remove(userId);
        }
        if (b.optBoolean("blue", false)) {
            blue.add(userId);
        } else {
            blue.remove(userId);
        }
    }

    private static void revoke(long userId) {
        remote.remove(userId);
        custom.remove(userId);
        blue.remove(userId);
    }

    private static void loadApi(int account) {
        io.execute(() -> {
            try {
                final java.net.HttpURLConnection c = open("/v1/badges");
                if (etag != null && apiOk) {
                    c.setRequestProperty("If-None-Match", etag);
                }
                final int code = c.getResponseCode();
                if (code == 304) {
                    apiOk = true;
                    return;
                }
                if (code != 200) {
                    apiOk = false;
                    return;
                }
                final JSONObject object = new JSONObject(readAll(c.getInputStream()));
                etag = c.getHeaderField("ETag");
                lastEventId = String.valueOf(object.optLong("rev", 0));
                final long def = parseId(object, "defaultEmojiId");
                if (def != 0) {
                    icon = def;
                }
                remote.clear();
                custom.clear();
                blue.clear();
                final JSONArray blueIds = object.optJSONArray("blueUserIds");
                for (int i = 0; blueIds != null && i < blueIds.length(); ++i) {
                    blue.add(blueIds.getLong(i));
                }
                final JSONArray list = object.optJSONArray("badges");
                final ArrayList<Long> ids = new ArrayList<>();
                for (int i = 0; list != null && i < list.length(); ++i) {
                    final JSONObject b = list.getJSONObject(i);
                    putBadge(b);
                    ids.add(b.optLong("userId", 0));
                }
                apiOk = true;
                fresh = true;
                android.util.Log.i("MokaBadges", "api ok rev=" + lastEventId + " badges=" + ids.size());
                saveCache(ids);
                refreshInterface(account);
                startSse(account);
            } catch (Exception e) {
                android.util.Log.i("MokaBadges", "api failed: " + e);
                apiOk = false;
            }
        });
    }

    private static void saveCache(ArrayList<Long> ids) {
        try {
            final JSONObject object = new JSONObject();
            object.put("emoji_id", icon);
            object.put("users", new JSONArray(ids));
            final JSONArray rows = new JSONArray();
            for (Map.Entry<Long, Custom> e : custom.entrySet()) {
                final JSONArray row = new JSONArray();
                row.put(e.getKey());
                row.put(e.getValue().icon);
                row.put(e.getValue().text);
                rows.put(row);
            }
            object.put("custom", rows);
            prefs().edit().putString("badges.cache", object.toString()).apply();
        } catch (Exception ignore) {
        }
    }

    private static void refreshInterface(int account) {
        handler.post(() -> {
            final MessagesController controller = MessagesController.getInstance(account);
            for (TLRPC.User user : controller.getUsers().values()) {
                if (applied.contains(user.id) && !has(user)) {
                    user.bot_verification_icon = 0; // галочку сняли
                    final TLRPC.UserFull full = controller.getUserFull(user.id);
                    if (full != null) {
                        full.bot_verification = null;
                    }
                    applied.remove(user.id);
                }
                if (has(user)) {
                    final long old = user.bot_verification_icon;
                    user.bot_verification_icon = 0;
                    apply(user);
                    final TLRPC.UserFull full = controller.getUserFull(user.id);
                    if (full != null && (old != user.bot_verification_icon || full.bot_verification != null)) {
                        full.bot_verification = null;
                        applyFull(user, full);
                    }
                }
            }
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
    }

    private static synchronized void startSse(int account) {
        if (sseStarted) {
            return;
        }
        sseStarted = true;
        io.execute(() -> {
            int delay = 3000;
            while (true) {
                try {
                    final java.net.HttpURLConnection c = open("/v1/events");
                    c.setRequestProperty("Accept", "text/event-stream");
                    if (lastEventId != null) {
                        c.setRequestProperty("Last-Event-ID", lastEventId);
                    }
                    c.setReadTimeout(60000); // нет ни байта (даже ping) 60 с: поток мёртв, переподключаемся
                    if (c.getResponseCode() == 200) {
                        sseAlive = true;
                        delay = 3000;
                        final java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream(), "UTF-8"));
                        String event = "message";
                        final StringBuilder data = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                if (data.length() > 0) {
                                    handleEvent(account, event, data.toString());
                                }
                                event = "message";
                                data.setLength(0);
                            } else if (line.startsWith("event:")) {
                                event = line.substring(6).trim();
                            } else if (line.startsWith("data:")) {
                                data.append(line.substring(5).trim());
                            } else if (line.startsWith("id:")) {
                                lastEventId = line.substring(3).trim();
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.i("MokaBadges", "sse: " + e);
                }
                sseAlive = false;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignore) {
                }
                delay = Math.min(delay * 2, 30000);
            }
        });
    }

    private static void handleEvent(int account, String event, String data) {
        try {
            final JSONObject o = new JSONObject(data);
            if ("hello".equals(event)) {
                if (o.optBoolean("resync", false)) {
                    apiOk = false;
                    loadApi(account);
                }
            } else if ("badge".equals(event)) {
                final String type = o.optString("type", "");
                final long userId = o.optLong("userId", 0);
                if ("revoked".equals(type)) {
                    revoke(userId);
                } else if (!o.isNull("badge")) {
                    putBadge(o.getJSONObject("badge"));
                }
                android.util.Log.i("MokaBadges", "event " + type + " " + userId);
                refreshInterface(account);
            }
        } catch (Exception e) {
            android.util.Log.i("MokaBadges", "event parse: " + e);
        }
    }
}
