package jp.puchicli.app.ui.home;

import android.content.Context;

import java.lang.reflect.Method;

/** Reads and writes the existing settings store. Manual start does not go through here. */
public final class EfficiencyStore {
    private static final String[] GETTERS = {
            "getIntervalMs",
            "getGestureDurationMs",
            "getTimeLimitMs",
            "getSwipeXRatio",
            "getSwipeEndXRatio",
            "getSwipeStartYRatio",
            "getSwipeEndYRatio",
            "getShowPath",
            "getTargetPackage",
            "getOverlayX",
            "getOverlayY",
            "getScheduleEnabled",
            "getScheduleHour",
            "getScheduleMinute",
            "getOneShotAtEpochMs",
            "getLastDelayMinutes",
            "getAutoDismissClose",
            "getAutoDismissCornerTap",
            "getAdFastScroll",
            "getSkipWithoutCatGauge",
            "getEndLockScreen",
            "getSwipeTravelPercent",
            "getSwipePathPreset",
            "getCheckinScheduleEnabled",
            "getCheckinHour",
            "getCheckinMinute",
            "getCheckinOneShotAtEpochMs",
            "getAdminPathSet",
            "getAdminStartXRatio",
            "getAdminStartYRatio",
            "getAdminEndXRatio",
            "getAdminEndYRatio",
            "getAdminTwoAmAtEpochMs",
            "getDailyCommitAtEpochMs",
            "getAdminUnlocked",
            "getAdminPathOverlayOn"
    };

    private EfficiencyStore() {
    }

    public static Object settingsStore(Context context) throws ReflectiveOperationException {
        Object app = context.getApplicationContext();
        return app.getClass().getMethod("getSettingsStore").invoke(app);
    }

    public static Object settings(Context context) throws ReflectiveOperationException {
        return settingsStore(context).getClass().getMethod("get").invoke(settingsStore(context));
    }

    public static boolean scheduleEnabled(Object settings) {
        return bool(settings, "getScheduleEnabled");
    }

    public static int scheduleHour(Object settings) {
        return integer(settings, "getScheduleHour");
    }

    public static int scheduleMinute(Object settings) {
        return integer(settings, "getScheduleMinute");
    }

    public static long oneShotAt(Object settings) {
        return longVal(settings, "getOneShotAtEpochMs");
    }

    public static long adminTwoAt(Object settings) {
        return longVal(settings, "getAdminTwoAmAtEpochMs");
    }

    public static boolean checkinEnabled(Object settings) {
        return bool(settings, "getCheckinScheduleEnabled");
    }

    public static int checkinHour(Object settings) {
        return integer(settings, "getCheckinHour");
    }

    public static int checkinMinute(Object settings) {
        return integer(settings, "getCheckinMinute");
    }

    public static long intervalMs(Object settings) {
        return longVal(settings, "getIntervalMs");
    }

    public static boolean customPath(Object settings) {
        if (bool(settings, "getAdminPathSet")) {
            return true;
        }
        Object preset = call(settings, "getSwipePathPreset");
        return preset != null && !"CENTER".equals(String.valueOf(preset));
    }

    public static int installDay(Context context) {
        try {
            Object store = settingsStore(context);
            Object day = store.getClass().getMethod("installDayNumber", long.class)
                    .invoke(store, System.currentTimeMillis());
            return day instanceof Integer ? (Integer) day : 1;
        } catch (ReflectiveOperationException e) {
            return 1;
        }
    }

    public static void setInstallDay(Context context, int day) throws ReflectiveOperationException {
        int clamped = Math.max(1, Math.min(999, day));
        Object store = settingsStore(context);
        store.getClass().getMethod("setInstallDayNumber", int.class, long.class)
                .invoke(store, clamped, System.currentTimeMillis());
    }

    public static boolean showCheckinSchedule() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.session.CheckinPolicy");
            Object instance = type.getField("INSTANCE").get(null);
            Object value = type.getMethod("showScheduleUi").invoke(instance);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static void setSchedule(Context context, boolean enabled, int hour, int minute)
            throws ReflectiveOperationException {
        Object current = settings(context);
        Object next = copy(current, new Object[][]{
                {"getScheduleEnabled", enabled},
                {"getScheduleHour", Math.max(0, Math.min(23, hour))},
                {"getScheduleMinute", Math.max(0, Math.min(59, minute))}
        });
        save(context, next, true);
    }

    public static void setCheckin(Context context, boolean enabled, int hour, int minute)
            throws ReflectiveOperationException {
        Object current = settings(context);
        Object next = copy(current, new Object[][]{
                {"getCheckinScheduleEnabled", enabled},
                {"getCheckinHour", Math.max(0, Math.min(23, hour))},
                {"getCheckinMinute", Math.max(0, Math.min(59, minute))}
        });
        save(context, next, true);
    }

    public static void cancelNextReservation(Context context) throws ReflectiveOperationException {
        Object current = settings(context);
        Object next = copy(current, new Object[][]{
                {"getScheduleEnabled", false},
                {"getOneShotAtEpochMs", 0L},
                {"getAdminTwoAmAtEpochMs", 0L},
                {"getDailyCommitAtEpochMs", 0L}
        });
        save(context, next, true);
    }

    public static void useDefaultPath(Context context) throws ReflectiveOperationException {
        Object current = settings(context);
        Object next = copy(current, new Object[][]{
                {"getSwipePathPreset", "CENTER"},
                {"getAdminPathSet", false},
                {"getSwipeXRatio", 0.165f},
                {"getSwipeEndXRatio", 0.1604f},
                {"getSwipeStartYRatio", 0.5569f},
                {"getSwipeEndYRatio", 0.2862f},
                {"getAdminStartXRatio", 0.165f},
                {"getAdminStartYRatio", 0.5569f},
                {"getAdminEndXRatio", 0.1604f},
                {"getAdminEndYRatio", 0.2862f}
        });
        save(context, next, false);
    }

    private static void save(Context context, Object settings, boolean sync) throws ReflectiveOperationException {
        Object store = settingsStore(context);
        store.getClass().getMethod("save", settings.getClass(), boolean.class).invoke(store, settings, sync);
    }

    private static Object copy(Object settings, Object[][] overrides) throws ReflectiveOperationException {
        Method copy = null;
        for (Method method : settings.getClass().getDeclaredMethods()) {
            if ("copy$default".equals(method.getName())) {
                copy = method;
                break;
            }
        }
        if (copy == null) {
            throw new NoSuchMethodException("copy$default");
        }
        copy.setAccessible(true);
        Class<?>[] types = copy.getParameterTypes();
        Object[] args = new Object[types.length];
        args[0] = settings;
        int mask1 = -1;
        int mask2 = -1;
        for (int i = 0; i < GETTERS.length; i++) {
            args[i + 1] = call(settings, GETTERS[i]);
        }
        for (Object[] override : overrides) {
            int index = indexOf(String.valueOf(override[0]));
            args[index + 1] = override[1];
            if (index < 32) {
                mask1 &= ~(1 << index);
            } else {
                mask2 &= ~(1 << (index - 32));
            }
        }
        args[args.length - 3] = mask1;
        args[args.length - 2] = mask2;
        args[args.length - 1] = null;
        return copy.invoke(null, args);
    }

    private static int indexOf(String getter) {
        for (int i = 0; i < GETTERS.length; i++) {
            if (GETTERS[i].equals(getter)) {
                return i;
            }
        }
        throw new IllegalArgumentException(getter);
    }

    private static Object call(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean bool(Object target, String name) {
        return Boolean.TRUE.equals(call(target, name));
    }

    private static int integer(Object target, String name) {
        Object value = call(target, name);
        return value instanceof Integer ? (Integer) value : 0;
    }

    private static long longVal(Object target, String name) {
        Object value = call(target, name);
        return value instanceof Long ? (Long) value : 0L;
    }
}
