package org.telegram.messenger.mokagram;

public final class GhostPolicy {
    public enum Event { READ, ONLINE, TYPING, STORY }

    private GhostPolicy() {
    }

    public static boolean insideSchedule(int now, int from, int to) {
        if (now < 0 || now >= 1440 || from < 0 || from >= 1440 || to < 0 || to >= 1440 || from == to) {
            return false;
        }
        return from < to ? now >= from && now < to : now >= from || now < to;
    }

    public static boolean hides(boolean enabled, boolean flag, boolean scheduled, boolean scheduledFlag) {
        return enabled && flag || scheduled && scheduledFlag;
    }
}
