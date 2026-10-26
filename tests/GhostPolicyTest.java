import org.telegram.messenger.mokagram.GhostPolicy;

public final class GhostPolicyTest {
    private static int checks;

    private static void expect(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check " + checks + " failed");
        }
    }

    public static void main(String[] args) {
        expect(!GhostPolicy.insideSchedule(599, 600, 720));
        expect(GhostPolicy.insideSchedule(600, 600, 720));
        expect(GhostPolicy.insideSchedule(719, 600, 720));
        expect(!GhostPolicy.insideSchedule(720, 600, 720));
        expect(GhostPolicy.insideSchedule(1380, 1380, 420));
        expect(GhostPolicy.insideSchedule(0, 1380, 420));
        expect(GhostPolicy.insideSchedule(419, 1380, 420));
        expect(!GhostPolicy.insideSchedule(420, 1380, 420));
        expect(!GhostPolicy.insideSchedule(100, 100, 100));
        expect(!GhostPolicy.insideSchedule(-1, 0, 1));
        expect(!GhostPolicy.insideSchedule(0, 1440, 0));
        for (int flags = 0; flags < 16; flags++) {
            boolean enabled = (flags & 1) != 0;
            boolean flag = (flags & 2) != 0;
            boolean scheduled = (flags & 4) != 0;
            boolean scheduledFlag = (flags & 8) != 0;
            boolean expected = (enabled && flag) || (scheduled && scheduledFlag);
            expect(GhostPolicy.hides(enabled, flag, scheduled, scheduledFlag) == expected);
        }
        System.out.println("GhostPolicy: " + checks + " checks passed");
    }
}
