package jp.puchicli.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import java.time.ZoneId;

/** Floating-panel status, stop reason, and the single start/stop tap. */
public final class EfficiencyOverlay {
    public static final String PAGE_EXTRA = "jp.puchicli.app.EFFICIENCY_PAGE";
    private static final long TAP_GAP_MS = 700L;

    private static Object heldState;
    private static String stickyReason;
    private static long lastTapMs;

    private EfficiencyOverlay() {
    }

    public static void holdState(Object state) {
        heldState = state;
    }

    public static void noteClosed() {
        stickyReason = "CLOSE";
    }

    public static String stickyReason() {
        return stickyReason;
    }

    public static void paint(Context service) {
        try {
            Object state = heldState != null ? heldState : readState();
            boolean running = bool(state, "getRunning");
            boolean stopping = bool(state, "getFinishing");
            boolean starting = isLaunching() && !running;
            View root = controlsView(service);
            if (root == null) {
                return;
            }
            Snapshot snap = snapshot(service, state, running, starting, stopping);
            setText(root, "efficiencyStatus", snap.phase);
            setText(root, "efficiencyNext", snap.detail);
            String perm = snap.permissionLine;
            if (!snap.missingNames.isEmpty()) {
                perm = perm + "\n" + snap.missingNames;
            }
            setText(root, "efficiencyPerm", perm);
            View card = find(root, "efficiencyStopCard");
            TextView reasonView = text(root, "efficiencyStopReason");
            TextView action = text(root, "efficiencyStopAction");
            boolean showReason = !running && !starting && !stopping && stickyReason != null
                    && !EfficiencyTexts.stopTitle(stickyReason).isEmpty();
            if (card != null) {
                card.setVisibility(showReason ? View.VISIBLE : View.GONE);
            }
            if (showReason && reasonView != null && action != null) {
                reasonView.setText(EfficiencyTexts.stopTitle(stickyReason));
                String label = EfficiencyTexts.stopAction(stickyReason, !snap.startAllowed, snap.accessibility);
                action.setText(label);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onReasonAction(v.getContext());
                    }
                });
            }
            if (!checkingIn(service)) {
                TextView label = text(root, "actionButtonLabel");
                View button = find(root, "actionButton");
                if (label != null) {
                    label.setText(EfficiencyTexts.primaryLabel(running, starting, stopping));
                }
                if (button != null) {
                    boolean busy = starting || stopping;
                    button.setEnabled(!busy);
                    button.setAlpha(busy ? 0.45f : 1f);
                    int bg = root.getResources().getIdentifier(
                            EfficiencyTexts.primaryStops(running, starting, stopping, false)
                                    ? "bg_btn_stop" : "bg_btn_start",
                            "drawable", root.getContext().getPackageName());
                    if (bg != 0) {
                        button.setBackgroundResource(bg);
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static String notificationLine(Context context, Object state, String original) {
        try {
            boolean running = bool(state, "getRunning");
            boolean stopping = bool(state, "getFinishing");
            boolean starting = isLaunching() && !running;
            Snapshot snap = snapshot(context, state, running, starting, stopping);
            String times = running ? snap.times : "";
            return EfficiencyTexts.notificationLine(snap.phase, snap.detail, times);
        } catch (RuntimeException e) {
            return original;
        }
    }

    public static void onPrimary(Context service) {
        if (!allowTap()) {
            return;
        }
        try {
            Object state = readState();
            boolean running = bool(state, "getRunning");
            boolean stopping = bool(state, "getFinishing");
            boolean starting = isLaunching();
            boolean checking = checkingIn(service);
            if (EfficiencyTexts.primaryStops(running, starting, stopping, checking)) {
                launch(service, "jp.puchicli.app.action.STOP", false);
                return;
            }
            if (!snapshot(service, state, false, false, false).startAllowed) {
                openSetup(service);
                return;
            }
            launch(service, "jp.puchicli.app.action.START", dimForManualStart());
        } catch (RuntimeException ignored) {
        }
    }

    public static void onReasonAction(Context context) {
        if (!allowTap()) {
            return;
        }
        Snapshot snap = snapshot(context, readState(), false, false, false);
        String label = EfficiencyTexts.stopAction(stickyReason, !snap.startAllowed, snap.accessibility);
        if (EfficiencyTexts.ACTION_SETTINGS.equals(label)) {
            openSetup(context);
            return;
        }
        try {
            launch(context, "jp.puchicli.app.action.START", dimForManualStart());
        } catch (RuntimeException ignored) {
        }
    }

    public static boolean allowTap() {
        if (isLaunching() || bool(readState(), "getFinishing")) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastTapMs < TAP_GAP_MS) {
            return false;
        }
        lastTapMs = now;
        return true;
    }

    public static void openSetup(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, "jp.puchicli.app.MainActivity");
            intent.putExtra("jp.puchicli.app.OPEN_SETTINGS", true);
            intent.putExtra(PAGE_EXTRA, "settings");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (RuntimeException ignored) {
        }
    }

    public static Snapshot snapshot(Context context) {
        Object state = readState();
        boolean running = bool(state, "getRunning");
        boolean stopping = bool(state, "getFinishing");
        boolean starting = isLaunching() && !running;
        return snapshot(context, state, running, starting, stopping);
    }

    public static final class Snapshot {
        public final String phase;
        public final String detail;
        public final String times;
        public final String permissionLine;
        public final String missingNames;
        public final boolean startAllowed;
        public final boolean accessibility;
        public final boolean running;
        public final boolean starting;
        public final boolean stopping;
        public final String reason;

        Snapshot(String phase, String detail, String times, String permissionLine, String missingNames,
                boolean startAllowed, boolean accessibility, boolean running, boolean starting,
                boolean stopping, String reason) {
            this.phase = phase;
            this.detail = detail;
            this.times = times;
            this.permissionLine = permissionLine;
            this.missingNames = missingNames;
            this.startAllowed = startAllowed;
            this.accessibility = accessibility;
            this.running = running;
            this.starting = starting;
            this.stopping = stopping;
            this.reason = reason;
        }
    }

    private static Snapshot snapshot(Context context, Object state, boolean running, boolean starting, boolean stopping) {
        boolean overlay = callBool("jp.puchicli.app.util.PermissionHelper", "canDrawOverlays", context);
        boolean accessibility = callBool("jp.puchicli.app.util.PermissionHelper", "accessibilityReady", context)
                && gestureBound();
        boolean lite = callBool("jp.puchicli.app.target.TargetApps", "anyKnownInstalled", context);
        boolean allowed = EfficiencyTexts.startAllowed(overlay, accessibility, lite);
        StringBuilder missing = new StringBuilder();
        int count = 0;
        if (!overlay) {
            count++;
            missing.append("他のアプリの上に表示");
        }
        if (!accessibility) {
            if (count > 0) {
                missing.append("、");
            }
            count++;
            missing.append("ユーザー補助");
        }
        if (!lite) {
            if (count > 0) {
                missing.append("、");
            }
            count++;
            missing.append("TikTok Lite");
        }
        if (running) {
            stickyReason = null;
        } else {
            String fromState = reasonName(state);
            if (fromState != null) {
                stickyReason = fromState;
            }
        }
        String phase = EfficiencyTexts.phase(running, starting, stopping);
        String swipe = "次のスワイプ：約0秒";
        String times = "";
        EfficiencyTexts.Reservation reservation = new EfficiencyTexts.Reservation(false, null, "次の予約：" + EfficiencyTexts.NO_RESERVATION);
        try {
            Object settings = EfficiencyStore.settings(context);
            swipe = EfficiencyTexts.nextSwipe(
                    EfficiencyStore.intervalMs(settings),
                    longVal(state, "getElapsedMs"),
                    longVal(state, "getLastSwipeAtElapsed"));
            if (running) {
                times = EfficiencyTexts.runningTimes(longVal(state, "getElapsedMs"), longVal(state, "getRemainingMs"));
            }
            reservation = EfficiencyTexts.reservation(
                    EfficiencyStore.scheduleEnabled(settings),
                    EfficiencyStore.scheduleHour(settings),
                    EfficiencyStore.scheduleMinute(settings),
                    EfficiencyStore.oneShotAt(settings),
                    EfficiencyStore.adminTwoAt(settings),
                    System.currentTimeMillis(),
                    ZoneId.systemDefault());
        } catch (ReflectiveOperationException ignored) {
        }
        String detail = EfficiencyTexts.detail(running, swipe, reservation.line);
        String reason = running ? null : stickyReason;
        return new Snapshot(phase, detail, times, EfficiencyTexts.permissionLine(count), missing.toString(),
                allowed, accessibility, running, starting, stopping, reason);
    }

    private static void launch(Context context, String action, boolean dim) {
        Class<?> type = classOrNull("jp.puchicli.app.overlay.OverlayService");
        if (type == null) {
            return;
        }
        try {
            Object companion = type.getField("Companion").get(null);
            companion.getClass().getMethod("launch", Context.class, String.class, boolean.class)
                    .invoke(companion, context, action, dim);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean dimForManualStart() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.overlay.ScreenDimPolicy");
            Object instance = type.getField("INSTANCE").get(null);
            Object value = type.getMethod("shouldDimForManualHomeStart").invoke(instance);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean isLaunching() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.overlay.OverlayService");
            Object companion = type.getField("Companion").get(null);
            Object value = companion.getClass().getMethod("isLaunching").invoke(companion);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean checkingIn(Context service) {
        try {
            Object value = service.getClass().getDeclaredField("checkingIn").get(service);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException e) {
            try {
                java.lang.reflect.Field field = service.getClass().getDeclaredField("checkingIn");
                field.setAccessible(true);
                Object value = field.get(service);
                return Boolean.TRUE.equals(value);
            } catch (ReflectiveOperationException again) {
                return false;
            }
        }
    }

    private static boolean gestureBound() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.a11y.GestureBridge");
            Object instance = type.getField("INSTANCE").get(null);
            return type.getMethod("getService").invoke(instance) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Object readState() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.session.SessionController");
            Object instance = type.getField("INSTANCE").get(null);
            Object flow = type.getMethod("getState").invoke(instance);
            return flow.getClass().getMethod("getValue").invoke(flow);
        } catch (ReflectiveOperationException e) {
            return heldState;
        }
    }

    private static View controlsView(Context service) {
        try {
            java.lang.reflect.Field field = service.getClass().getDeclaredField("controlsView");
            field.setAccessible(true);
            Object value = field.get(service);
            return value instanceof View ? (View) value : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean callBool(String className, String method, Context context) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getField("INSTANCE").get(null);
            Object value = type.getMethod(method, Context.class).invoke(instance, context);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean bool(Object target, String name) {
        if (target == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(target.getClass().getMethod(name).invoke(target));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static long longVal(Object target, String name) {
        if (target == null) {
            return 0L;
        }
        try {
            Object value = target.getClass().getMethod(name).invoke(target);
            return value instanceof Long ? (Long) value : 0L;
        } catch (ReflectiveOperationException e) {
            return 0L;
        }
    }

    private static String reasonName(Object state) {
        if (state == null) {
            return null;
        }
        try {
            Object reason = state.getClass().getMethod("getLastStopReason").invoke(state);
            if (reason == null) {
                return null;
            }
            return String.valueOf(reason.getClass().getMethod("name").invoke(reason));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static View find(View root, String name) {
        int id = root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
        if (id == 0) {
            return null;
        }
        return root.findViewById(id);
    }

    private static TextView text(View root, String name) {
        View view = find(root, name);
        return view instanceof TextView ? (TextView) view : null;
    }

    private static void setText(View root, String name, String value) {
        TextView view = text(root, name);
        if (view != null) {
            view.setText(value);
        }
    }
}
