package jp.puchicli.app.ui.home;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Shared labels for the home card, the floating panel, and the notification. */
public final class EfficiencyTexts {
    public static final String PHASE_RUNNING = "実行中";
    public static final String PHASE_IDLE = "待機";
    public static final String PHASE_STARTING = "開始中";
    public static final String PHASE_STOPPING = "停止中";
    public static final String NO_RESERVATION = "予約なし";
    public static final String READY = "準備完了";
    public static final String ACTION_SETTINGS = "権限を設定";
    public static final String ACTION_RESUME = "再開";
    public static final String ACTION_FRESH = "新しく開始";
    public static final String START = "開始";
    public static final String STOP = "停止";

    private EfficiencyTexts() {
    }

    public static String phase(boolean running, boolean starting, boolean stopping) {
        if (stopping) {
            return PHASE_STOPPING;
        }
        if (starting && !running) {
            return PHASE_STARTING;
        }
        if (running) {
            return PHASE_RUNNING;
        }
        return PHASE_IDLE;
    }

    public static String primaryLabel(boolean running, boolean starting, boolean stopping) {
        if (stopping) {
            return PHASE_STOPPING;
        }
        if (starting && !running) {
            return PHASE_STARTING;
        }
        if (running || starting) {
            return STOP;
        }
        return START;
    }

    public static boolean primaryStops(boolean running, boolean starting, boolean stopping, boolean checkingIn) {
        return running || starting || stopping || checkingIn;
    }

    public static String nextSwipe(long intervalMs, long elapsedMs, long lastSwipeAtElapsed) {
        long since = Math.max(0L, elapsedMs - Math.max(0L, lastSwipeAtElapsed));
        long left = intervalMs - since;
        if (left < 0L) {
            left = 0L;
        }
        long seconds = (left + 999L) / 1000L;
        return "次のスワイプ：約" + seconds + "秒";
    }

    public static String hms(long ms) {
        long total = Math.max(0L, ms) / 1000L;
        long h = total / 3600L;
        long m = (total % 3600L) / 60L;
        long s = total % 60L;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public static String runningTimes(long elapsedMs, long remainingMs) {
        return "経過 " + hms(elapsedMs) + "　残り " + hms(remainingMs);
    }

    public static String hhmm(int hour, int minute) {
        int h = Math.max(0, Math.min(23, hour));
        int m = Math.max(0, Math.min(59, minute));
        return String.format("%02d:%02d", h, m);
    }

    public static boolean isPending(long epochMs, long nowMs) {
        return epochMs > 0L && epochMs + 1000L >= nowMs;
    }

    public static long nextDailyMillis(long nowMs, int hour, int minute, ZoneId zone) {
        ZonedDateTime now = Instant.ofEpochMilli(nowMs).atZone(zone);
        ZonedDateTime at = now.withHour(Math.max(0, Math.min(23, hour)))
                .withMinute(Math.max(0, Math.min(59, minute)))
                .withSecond(0)
                .withNano(0);
        if (!at.isAfter(now)) {
            at = at.plusDays(1L);
        }
        return at.toInstant().toEpochMilli();
    }

    public static String clock(long epochMs, ZoneId zone) {
        ZonedDateTime at = Instant.ofEpochMilli(epochMs).atZone(zone);
        return hhmm(at.getHour(), at.getMinute());
    }

    public static final class Reservation {
        public final boolean present;
        public final String hhmm;
        public final String line;

        public Reservation(boolean present, String hhmm, String line) {
            this.present = present;
            this.hhmm = hhmm;
            this.line = line;
        }
    }

    public static Reservation reservation(boolean scheduleEnabled, int hour, int minute,
            long oneShotAt, long adminTwoAt, long nowMs, ZoneId zone) {
        Long soonest = null;
        if (scheduleEnabled) {
            soonest = nextDailyMillis(nowMs, hour, minute, zone);
        }
        if (isPending(oneShotAt, nowMs)) {
            soonest = sooner(soonest, oneShotAt);
        }
        if (isPending(adminTwoAt, nowMs)) {
            soonest = sooner(soonest, adminTwoAt);
        }
        if (soonest == null) {
            return new Reservation(false, null, "次の予約：" + NO_RESERVATION);
        }
        String time = clock(soonest, zone);
        return new Reservation(true, time, "次の予約：" + time);
    }

    private static Long sooner(Long current, long candidate) {
        if (current == null || candidate < current) {
            return candidate;
        }
        return current;
    }

    public static String permissionLine(int missingCount) {
        if (missingCount <= 0) {
            return READY;
        }
        return "権限NG " + missingCount + "件";
    }

    public static boolean startAllowed(boolean overlay, boolean accessibility, boolean liteInstalled) {
        return overlay && accessibility && liteInstalled;
    }

    public static String stopTitle(String reason) {
        if ("USER".equals(reason)) {
            return "手動で停止しました";
        }
        if ("TIME_LIMIT".equals(reason)) {
            return "制限時間に達しました";
        }
        if ("ACCESSIBILITY_LOST".equals(reason)) {
            return "ユーザー補助がオフです";
        }
        if ("CLOSE".equals(reason)) {
            return "パネルを閉じました";
        }
        return "";
    }

    public static String stopAction(String reason, boolean permissionBlocked, boolean accessibilityReady) {
        if (permissionBlocked || ("ACCESSIBILITY_LOST".equals(reason) && !accessibilityReady)) {
            return ACTION_SETTINGS;
        }
        if ("TIME_LIMIT".equals(reason)) {
            return ACTION_FRESH;
        }
        return ACTION_RESUME;
    }

    public static String detail(boolean running, String nextSwipe, String reservationLine) {
        if (running) {
            return nextSwipe;
        }
        return reservationLine;
    }

    public static String notificationLine(String phase, String detail, String timesOrEmpty) {
        if (timesOrEmpty == null || timesOrEmpty.isEmpty()) {
            return phase + " · " + detail;
        }
        return phase + " · " + detail + " · " + timesOrEmpty;
    }

    public static String pathValue(boolean custom) {
        return custom ? "カスタム" : "既定";
    }

    public static String onOff(boolean on) {
        return on ? "オン" : "オフ";
    }
}
